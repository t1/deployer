package com.github.t1.deployer;

import static java.util.concurrent.TimeUnit.*;
import static javax.ws.rs.core.Response.Status.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.*;
import org.jboss.as.controller.client.helpers.standalone.*;
import org.jboss.dmr.ModelNode;

import com.github.t1.log.Logged;

@Slf4j
@Logged
public class DeploymentsContainer {
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
        private final String contextRoot;
        private final InputStream inputStream;

        @Override
        protected DeploymentPlanBuilder buildPlan(InitialDeploymentPlanBuilder plan) {
            return plan //
                    .add(contextRoot, inputStream) //
                    .deploy(contextRoot) //
            ;
        }
    }

    @AllArgsConstructor
    private class ReplacePlan extends AbstractPlan {
        private final String contextRoot;
        private final InputStream inputStream;

        @Override
        protected DeploymentPlanBuilder buildPlan(InitialDeploymentPlanBuilder plan) {
            return plan.replace(contextRoot, inputStream);
        }
    }

    @AllArgsConstructor
    private class UndeployPlan extends AbstractPlan {
        private final String deploymentName;

        @Override
        protected DeploymentPlanBuilder buildPlan(InitialDeploymentPlanBuilder plan) {
            return plan //
                    .undeploy(deploymentName) //
                    .remove(deploymentName) //
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

    @Inject
    Repository repository;

    @SneakyThrows(IOException.class)
    private ModelNode execute(ModelNode command) {
        log.debug("execute command {}", command);
        ModelNode result = client.execute(command, LOGGING);
        log.debug("-> {}", result);
        return result;
    }

    private void checkOutcome(ModelNode result) {
        String outcome = result.get("outcome").asString();
        if (!"success".equals(outcome)) {
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
        }
    }

    public Deployment getDeploymentByContextRoot(String contextRoot) {
        ModelNode node = readDeployments(contextRoot + ".war"); // TODO get name from CLI
        Deployment deployment = toDeployment(node);
        check(deployment, contextRoot);

        log.debug("found deployment {}", deployment);
        return deployment;
    }

    private void check(Deployment deployment, String contextRoot) {
        if (!deployment.getContextRoot().equals(contextRoot)) {
            log.debug("deployment context root {} doesn't match {}", deployment.getContextRoot(), contextRoot);
            throw new WebApplicationException(NOT_FOUND);
        }
    }

    public List<Deployment> getAllDeployments() {
        List<Deployment> list = new ArrayList<>();
        for (ModelNode cliDeploymentMatch : readDeployments("*").asList())
            list.add(toDeployment(cliDeploymentMatch.get("result")));
        return list;
    }

    private ModelNode readDeployments(String name) {
        ModelNode result = execute(readDeploymentModel(name));
        checkOutcome(result);
        return result.get("result");
    }

    public static ModelNode readDeploymentModel(String deployment) {
        ModelNode node = new ModelNode();
        node.get("address").add("deployment", deployment);
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private Deployment toDeployment(ModelNode cliDeployment) {
        String name = cliDeployment.get("name").asString();
        String contextRoot = getContextRoot(cliDeployment);
        CheckSum hash = CheckSum.of(cliDeployment.get("content").get(0).get("hash").asBytes());
        log.debug("{} -> {} -> {}", name, contextRoot, hash);
        return new Deployment(name, contextRoot, hash);
    }

    private String getContextRoot(ModelNode cliDeployment) {
        ModelNode subsystems = cliDeployment.get("subsystem");
        // JBoss 8 uses 'undertow' while JBoss 7 uses 'web'
        ModelNode web = (subsystems.has("web")) ? subsystems.get("web") : subsystems.get("undertow");
        return web.get("context-root").asString().substring(1);
    }

    public void deploy(String contextRoot, InputStream deployment) {
        new ReplacePlan(contextRoot, deployment).execute();
    }

    public void undeploy(String deploymentName) {
        new UndeployPlan(deploymentName).execute();
    }
}
