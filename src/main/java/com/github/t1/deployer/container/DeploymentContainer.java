package com.github.t1.deployer.container;

import static com.github.t1.log.LogLevel.*;
import static com.github.t1.ramlap.ProblemDetail.*;
import static java.util.concurrent.TimeUnit.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.annotation.security.*;
import javax.ejb.Stateless;

import org.jboss.as.controller.client.helpers.standalone.*;
import org.jboss.dmr.ModelNode;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Logged(level = INFO)
@Stateless
public class DeploymentContainer extends AbstractContainer {
    public static final ContextRoot UNDEFINED_CONTEXT_ROOT = new ContextRoot("?");

    private abstract class AbstractPlan {
        public void execute() {
            try (ServerDeploymentManager deploymentManager = ServerDeploymentManager.Factory.create(client)) {
                DeploymentPlan plan = buildPlan(deploymentManager.newDeploymentPlan()).build();

                log.debug("start executing {}", getClass().getSimpleName());
                logDeployPlan(plan);
                Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
                log.debug("wait for {}", getClass().getSimpleName());
                ServerDeploymentPlanResult result = future.get(30, SECONDS);
                log.debug("done executing {}", getClass().getSimpleName());

                checkOutcome(plan, result);
            } catch (IOException | ExecutionException | TimeoutException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        protected abstract DeploymentPlanBuilder buildPlan(InitialDeploymentPlanBuilder plan);

        private void logDeployPlan(DeploymentPlan plan) {
            if (!log.isTraceEnabled())
                return;
            for (DeploymentAction action : plan.getDeploymentActions()) {
                log.trace("- planned action: {} {} -> {}", action.getType(), action.getDeploymentUnitUniqueName(),
                        action.getReplacedDeploymentUnitUniqueName());
            }
        }

        private void checkOutcome(DeploymentPlan plan, ServerDeploymentPlanResult result) {
            boolean failed = false;
            Throwable firstThrowable = null;
            for (DeploymentAction action : plan.getDeploymentActions()) {
                ServerDeploymentActionResult actionResult = result.getDeploymentActionResult(action.getId());
                Throwable deploymentException = actionResult.getDeploymentException();
                if (deploymentException != null)
                    firstThrowable = deploymentException;
                switch (actionResult.getResult()) {
                    case CONFIGURATION_MODIFIED_REQUIRES_RESTART:
                        log.warn("requries restart: {}: {}", action.getType(), action.getDeploymentUnitUniqueName());
                        break;
                    case EXECUTED:
                        log.debug("executed: {}: {}", action.getType(), action.getDeploymentUnitUniqueName());
                        break;
                    case FAILED:
                        failed = true;
                        log.error("failed: {}: {}", action.getType(), action.getDeploymentUnitUniqueName());
                        break;
                    case NOT_EXECUTED:
                        log.debug("not executed: {}: {}", action.getType(), action.getDeploymentUnitUniqueName());
                        break;
                    case ROLLED_BACK:
                        log.debug("rolled back: {}: {}", action.getType(), action.getDeploymentUnitUniqueName());
                        break;
                }
            }
            if (firstThrowable != null || failed) {
                throw new RuntimeException("failed to execute " + getClass().getSimpleName(), firstThrowable);
            }
        }
    }

    @AllArgsConstructor
    private class DeployPlan extends AbstractPlan {
        private final DeploymentName deploymentName;
        private final InputStream inputStream;

        @Override
        protected DeploymentPlanBuilder buildPlan(InitialDeploymentPlanBuilder plan) {
            return plan //
                    .add(deploymentName.getValue(), inputStream) //
                    .deploy(deploymentName.getValue()) //
                    ;
        }
    }

    @AllArgsConstructor
    private class ReplacePlan extends AbstractPlan {
        private final DeploymentName deploymentName;
        private final InputStream inputStream;

        @Override
        protected DeploymentPlanBuilder buildPlan(InitialDeploymentPlanBuilder plan) {
            return plan.replace(deploymentName.getValue(), inputStream);
        }
    }

    @AllArgsConstructor
    private class UndeployPlan extends AbstractPlan {
        private final DeploymentName deploymentName;

        @Override
        protected DeploymentPlanBuilder buildPlan(InitialDeploymentPlanBuilder plan) {
            return plan //
                    .undeploy(deploymentName.getValue()) //
                    .remove(deploymentName.getValue()) //
                    ;
        }
    }

    @PermitAll
    public boolean hasDeploymentWith(ContextRoot contextRoot) {
        for (Deployment deployment : getAllDeployments()) {
            if (deployment.getContextRoot().equals(contextRoot)) {
                return true;
            }
        }
        return false;
    }

    @PermitAll
    public Deployment getDeploymentFor(ContextRoot contextRoot) {
        List<Deployment> all = getAllDeployments();
        Deployment deployment = find(all, contextRoot);
        log.debug("found deployment {}", deployment);
        return deployment;
    }

    private Deployment find(List<Deployment> all, ContextRoot contextRoot) {
        for (Deployment deployment : all) {
            if (deployment.getContextRoot().equals(contextRoot)) {
                return deployment;
            }
        }
        throw notFound("no deployment with context root [" + contextRoot + "]");
    }

    @PermitAll
    public Deployment getDeploymentWith(CheckSum checkSum) {
        List<Deployment> all = getAllDeployments();
        Deployment deployment = find(all, checkSum);
        log.debug("found deployment {}", deployment);
        return deployment;
    }

    private Deployment find(List<Deployment> all, CheckSum checkSum) {
        for (Deployment deployment : all) {
            if (deployment.getCheckSum().equals(checkSum)) {
                return deployment;
            }
        }
        throw notFound("no deployment with context root [" + checkSum + "]");
    }

    @PermitAll
    public List<Deployment> getAllDeployments() {
        List<Deployment> list = new ArrayList<>();
        for (ModelNode cliDeploymentMatch : readAllDeployments())
            list.add(toDeployment(cliDeploymentMatch.get("result")));
        return list;
    }

    private List<ModelNode> readAllDeployments() {
        ModelNode result = execute(readDeployments());
        checkOutcome(result);
        return result.get("result").asList();
    }

    private static ModelNode readDeployments() {
        ModelNode node = new ModelNode();
        node.get("address").add("deployment", "*");
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private Deployment toDeployment(ModelNode cliDeployment) {
        DeploymentName name = new DeploymentName(cliDeployment.get("name").asString());
        ContextRoot contextRoot = getContextRoot(cliDeployment);
        CheckSum hash = CheckSum.of(hash(cliDeployment));
        log.debug("{} -> {} -> {}", name, contextRoot, hash);
        return new Deployment(name, contextRoot, hash, null);
    }

    private byte[] hash(ModelNode cliDeployment) {
        try {
            return cliDeployment.get("content").get(0).get("hash").asBytes();
        } catch (RuntimeException e) {
            log.error("failed to get hash for {}", cliDeployment.get("name"));
            return new byte[0];
        }
    }

    private ContextRoot getContextRoot(ModelNode cliDeployment) {
        ModelNode subsystems = cliDeployment.get("subsystem");
        // JBoss 8+ uses 'undertow' while JBoss 7 uses 'web'
        ModelNode web = (subsystems.has("web")) ? subsystems.get("web") : subsystems.get("undertow");
        ModelNode contextRoot = web.get("context-root");
        return toContextRoot(contextRoot);
    }

    private ContextRoot toContextRoot(ModelNode contextRoot) {
        if (!contextRoot.isDefined())
            return UNDEFINED_CONTEXT_ROOT;
        return new ContextRoot(contextRoot.asString().substring(1)); // strip leading slash
    }

    @DeploymentOperation
    @RolesAllowed("deployer")
    public void deploy(DeploymentName deploymentName, InputStream inputStream) {
        new DeployPlan(deploymentName, inputStream).execute();
    }

    @DeploymentOperation
    @RolesAllowed("deployer")
    public void redeploy(DeploymentName deploymentName, InputStream inputStream) {
        new ReplacePlan(deploymentName, inputStream).execute();
    }

    @DeploymentOperation
    @RolesAllowed("deployer")
    public void undeploy(DeploymentName deploymentName) {
        new UndeployPlan(deploymentName).execute();
    }
}
