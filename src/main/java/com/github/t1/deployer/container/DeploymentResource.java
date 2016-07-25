package com.github.t1.deployer.container;

import com.github.t1.deployer.model.Checksum;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.helpers.standalone.*;
import org.jboss.dmr.ModelNode;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;

import static com.github.t1.deployer.container.CLI.*;
import static com.github.t1.deployer.container.DeploymentName.*;
import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Builder(builderMethodName = "doNotCallThisBuilderExternally")
@Accessors(fluent = true, chain = true)
public class DeploymentResource extends AbstractResource {
    private static final int TIMEOUT = 30;

    @NonNull @Getter private final DeploymentName name;
    private Checksum checksum;
    private InputStream inputStream;

    public DeploymentResource(DeploymentName name, CLI cli) {
        super(cli);
        this.name = name;
    }

    public static DeploymentResourceBuilder builder(DeploymentName name, CLI cli) {
        return doNotCallThisBuilderExternally().name(name).container(cli);
    }

    public static List<DeploymentResource> allDeployments(CLI cli) {
        return cli.execute(readResource(new DeploymentResource(ALL, cli).createRequestWithAddress()))
                  .asList().stream()
                  .map(match -> toDeployment(match.get("result"), cli))
                  .collect(toList());
    }

    private static DeploymentResource toDeployment(ModelNode node, CLI cli) {
        DeploymentName name = readName(node);
        Checksum hash = readHash(node);
        log.debug("read from all deployments {}: {}", name, hash);
        return DeploymentResource.builder(name, cli).checksum(hash).build();
    }

    public static class DeploymentResourceBuilder {
        private CLI cli;

        public DeploymentResourceBuilder container(CLI cli) {
            this.cli = cli;
            return this;
        }

        public DeploymentResource build() {
            DeploymentResource resource = new DeploymentResource(name, cli);
            resource.inputStream = inputStream;
            resource.checksum = checksum;
            return resource;
        }
    }

    @Override public String toString() {
        return name
                + ((checksum == null) ? "" : ":" + checksum)
                + ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed");
    }

    public Checksum checksum() {
        checkDeployed();
        return checksum;
    }

    @Override protected ModelNode createRequestWithAddress() {
        ModelNode request = new ModelNode();
        request.get("address").add("deployment", name.getValue());
        return request;
    }

    @Override protected void readFrom(ModelNode node) {
        DeploymentName name = readName(node);
        Checksum checksum = readHash(node);
        log.debug("read deployment {}: {}", name, checksum);
        assert this.name.equals(name);
        this.checksum = checksum;
    }

    private static DeploymentName readName(ModelNode node) { return new DeploymentName(node.get("name").asString()); }

    private static Checksum readHash(ModelNode node) { return Checksum.of(hash(node)); }

    private static byte[] hash(ModelNode cliDeployment) {
        try {
            return cliDeployment.get("content").get(0).get("hash").asBytes();
        } catch (RuntimeException e) {
            log.error("failed to get hash for {}", cliDeployment.get("name"));
            return new byte[0];
        }
    }

    private abstract class AbstractPlan {
        public void execute() {
            try (ServerDeploymentManager deploymentManager = openServerDeploymentManager()) {
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
            String name = deploymentName.getValue();
            return plan.add(name, inputStream).deploy(name);
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
            String name = deploymentName.getValue();
            return plan.undeploy(name).remove(name);
        }
    }

    @Override public void add() {
        assert inputStream != null : "need an input stream to deploy";
        new DeployPlan(name, inputStream).execute();
        this.deployed = true;
    }

    public void redeploy(InputStream inputStream) {
        new ReplacePlan(name, inputStream).execute();
    }

    @Override public void remove() {
        new UndeployPlan(name).execute();
    }
}
