package com.github.t1.deployer.container;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.helpers.standalone.*;
import org.jboss.dmr.ModelNode;

import javax.ejb.Stateless;
import java.io.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.github.t1.log.LogLevel.*;
import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Logged(level = INFO)
@Stateless
public class DeploymentContainer extends CLI {
    private static final int TIMEOUT = 30;

    public static final ContextRoot UNDEFINED_CONTEXT_ROOT = new ContextRoot("?");

    private abstract class AbstractPlan {
        public void execute() {
            try (ServerDeploymentManager deploymentManager = ServerDeploymentManager.Factory.create(client)) {
                DeploymentPlan plan = buildPlan(deploymentManager.newDeploymentPlan()).build();

                log.debug("start executing {}", getClass().getSimpleName());
                logDeployPlan(plan);
                Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
                log.debug("wait for {}", getClass().getSimpleName());
                ServerDeploymentPlanResult result = future.get(TIMEOUT, SECONDS);
                log.debug("done executing {}", getClass().getSimpleName());

                checkOutcome(plan, result);
            } catch (IOException | ExecutionException | TimeoutException | InterruptedException e) {
                throw new RuntimeException("deployment failed", e);
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
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                Throwable deploymentException = actionResult.getDeploymentException();
                if (deploymentException != null)
                    firstThrowable = deploymentException;
                switch (actionResult.getResult()) {
                case CONFIGURATION_MODIFIED_REQUIRES_RESTART:
                    log.warn("requires restart: {}: {}", action.getType(), action.getDeploymentUnitUniqueName());
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
            return plan
                    .add(deploymentName.getValue(), inputStream)
                    .deploy(deploymentName.getValue());
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
            return plan
                    .undeploy(deploymentName.getValue())
                    .remove(deploymentName.getValue());
        }
    }

    public boolean hasDeployment(ContextRoot contextRoot) { return all().anyMatch(contextRoot::matches); }

    public boolean hasDeployment(DeploymentName deploymentName) { return all().anyMatch(deploymentName::matches); }

    public boolean hasDeployment(CheckSum checkSum) { return all().anyMatch(checkSum::matches); }

    public Deployment getDeployment(ContextRoot contextRoot) {
        return all().filter(contextRoot::matches).findAny()
                    .orElseThrow(() -> new RuntimeException("no deployment with context root [" + contextRoot + "]"));
    }

    public Deployment getDeployment(DeploymentName name) {
        return all().filter(name::matches).findAny()
                    .orElseThrow(() -> new RuntimeException("no deployment with name [" + name + "]"));
    }

    public Deployment getDeployment(CheckSum checkSum) {
        return all().filter(checkSum::matches).findAny()
                    .orElseThrow(() -> new RuntimeException("no deployment with checksum [" + checkSum + "]"));
    }

    private Stream<Deployment> all() { return getAllDeployments().stream(); }

    public List<Deployment> getAllDeployments() {
        return execute(readDeployments())
                .asList().stream()
                .map(cliDeploymentMatch -> toDeployment(cliDeploymentMatch.get("result")))
                .collect(toList());
    }

    private static ModelNode readDeployments() {
        ModelNode request = new ModelNode();
        request.get("address").add("deployment", "*");
        return readResource(request);
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

    public void deploy(DeploymentName deploymentName, InputStream inputStream) {
        new DeployPlan(deploymentName, inputStream).execute();
    }

    public void redeploy(DeploymentName deploymentName, InputStream inputStream) {
        new ReplacePlan(deploymentName, inputStream).execute();
    }

    public void undeploy(DeploymentName deploymentName) {
        new UndeployPlan(deploymentName).execute();
    }
}
