package com.github.t1.deployer.container;

import java.net.InetAddress;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.*;

@Slf4j
public class JBossCliTestClient {
    public static void main(String[] args) throws Exception {
        InetAddress host = InetAddress.getByName("127.0.0.1");
        int port = 9999;
        log.info("connect to JBoss AS on {}:{}", host, port);

        // File file = new File("foo.war").getCanonicalFile();
        // log.info("undeploy {}", file);

        try (ModelControllerClient client = ModelControllerClient.Factory.create(host, port)) {
            try (ServerDeploymentManager deploymentManager = ServerDeploymentManager.Factory.create(client)) {
                DeploymentPlan plan =
                        deploymentManager.newDeploymentPlan().undeploy("foo.war").remove("foo.war").build();

                Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
                ServerDeploymentPlanResult result = future.get();

                checkOutcome(plan, result);
            }
        }
    }

    private static void checkOutcome(DeploymentPlan plan, ServerDeploymentPlanResult result) {
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
            throw new RuntimeException("failed to execute " + plan.getClass().getSimpleName(), firstThrowable);
        }
    }
}
