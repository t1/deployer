package com.github.t1.deployer.container;

import static com.github.t1.deployer.tools.WebException.*;
import static java.util.concurrent.TimeUnit.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.ejb.Stateless;
import javax.inject.Inject;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.*;
import org.jboss.as.controller.client.helpers.standalone.*;
import org.jboss.dmr.ModelNode;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;

@Slf4j
@Logged
@Stateless
public class Container {
    public static final ContextRoot UNDEFINED_CONTEXT_ROOT = new ContextRoot("?");

    private abstract class AbstractPlan {
        public void execute() {
            try (ServerDeploymentManager deploymentManager = ServerDeploymentManager.Factory.create(client)) {
                DeploymentPlan plan = buildPlan(deploymentManager.newDeploymentPlan()).build();

                log.debug("start executing {}", getClass().getSimpleName());
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

    private static final OperationMessageHandler LOGGING = new OperationMessageHandler() {
        @Override
        public void handleReport(MessageSeverity severity, String message) {
            switch (severity) {
                case ERROR:
                    log.error(message);
                case WARN:
                    log.warn(message);
                    break;
                case INFO:
                    log.info(message);
                    break;
            }
        }
    };

    @Inject
    ModelControllerClient client;

    @SneakyThrows(IOException.class)
    private ModelNode execute(ModelNode command) {
        log.debug("execute command {}", command);
        ModelNode result = client.execute(command, LOGGING);
        log.trace("-> {}", result);
        return result;
    }

    private void checkOutcome(ModelNode result) {
        String outcome = result.get("outcome").asString();
        if (!"success".equals(outcome)) {
            log.error("failed: {}", result);
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
        }
    }

    public boolean hasDeploymentWith(ContextRoot contextRoot) {
        for (Deployment deployment : getAllDeployments()) {
            if (deployment.getContextRoot().equals(contextRoot)) {
                return true;
            }
        }
        return false;
    }

    public Deployment getDeploymentWith(ContextRoot contextRoot) {
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
        CheckSum hash = CheckSum.of(cliDeployment.get("content").get(0).get("hash").asBytes());
        log.debug("{} -> {} -> {}", name, contextRoot, hash);
        return new Deployment(name, contextRoot, hash);
    }

    private ContextRoot getContextRoot(ModelNode cliDeployment) {
        ModelNode subsystems = cliDeployment.get("subsystem");
        // JBoss 8 uses 'undertow' while JBoss 7 uses 'web'
        ModelNode web = (subsystems.has("web")) ? subsystems.get("web") : subsystems.get("undertow");
        ModelNode contextRoot = web.get("context-root");
        return toContextRoot(contextRoot);
    }

    private ContextRoot toContextRoot(ModelNode contextRoot) {
        if (!contextRoot.isDefined())
            return UNDEFINED_CONTEXT_ROOT;
        return new ContextRoot(contextRoot.asString().substring(1)); // strip leading slash
    }

    @ContainerDeployment
    public void deploy(Deployment deployment, InputStream inputStream) {
        new DeployPlan(deployment.getName(), inputStream).execute();
    }

    @ContainerDeployment
    public void redeploy(Deployment deployment, InputStream inputStream) {
        new ReplacePlan(deployment.getName(), inputStream).execute();
    }

    @ContainerDeployment
    public void undeploy(Deployment deployment) {
        new UndeployPlan(deployment.getName()).execute();
    }
}
