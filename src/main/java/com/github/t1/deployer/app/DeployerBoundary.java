package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Variables.UnresolvedVariableException;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.log.Logged;
import com.github.t1.problem.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.*;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.log.LogLevel.*;
import static java.util.Collections.*;
import static javax.ws.rs.core.Response.Status.*;

@javax.ws.rs.Path("/")
@Stateless
@Logged(level = INFO)
@Slf4j
public class DeployerBoundary {
    public static final String ROOT_BUNDLE = "deployer.root.bundle";
    private static final String DEFAULT_ROOT_BUNDLE = ""
            + "bundles:\n"
            + "  ${regex(root-bundle-name or hostName(), bundle-to-host-name or «(.*?)\\d*»)}:\n"
            + "    group-id: ${root-bundle-group or default.group-id or domainName()}\n"
            + "    classifier: ${root-bundle-classifier or null}\n"
            + "    version: ${root-bundle-version or version}\n";

    public java.nio.file.Path getRootBundlePath() { return container.getConfigDir().resolve(ROOT_BUNDLE); }

    @GET
    public ConfigurationPlan getEffectivePlan() { return new Run().read(); }

    /** see {@link Audits} */
    @Value
    public static class AuditsResponse {
        List<Audit> audits;
    }

    @POST
    public AuditsResponse post(Map<String, String> form) { return new AuditsResponse(apply(form).getAudits()); }

    @Asynchronous
    @SneakyThrows(InterruptedException.class)
    public void applyAsync() {
        Thread.sleep(1000);
        try {
            apply(emptyMap());
        } catch (UnresolvedVariableException e) {
            // not really nice, but seems - over all - better than splitting and repeating the overall control flow
            log.info("skip async run for unresolved variable: {}", e.getExpression());
        }
    }

    public synchronized Audits apply(Map<String, String> variables) {
        Run run = new Run().withVariables(variables);

        Path root = getRootBundlePath();
        if (Files.isRegularFile(root)) {
            run.apply(root);
        } else {
            run.applyDefaultRoot();
        }

        if (log.isDebugEnabled())
            log.debug("\n{}", audits.toYaml());
        return audits;
    }


    @Inject Container container;
    @Inject Repository repository;

    @Inject @Config("variables") Map<String, String> configuredVariables;

    @Inject Audits audits;
    // TODO @Inject Instance<AbstractDeployer> deployers;
    @Inject DeployableDeployer deployableDeployer;
    @Inject LogHandlerDeployer logHandlerDeployer;
    @Inject LoggerDeployer loggerDeployer;


    private class Run {
        private Variables variables = new Variables().withAll(configuredVariables);

        public ConfigurationPlan read() {
            ConfigurationPlanBuilder builder = ConfigurationPlan.builder();
            // TODO deployers.forEach(deployer -> deployer.read(builder));
            logHandlerDeployer.read(builder);
            loggerDeployer.read(builder);
            deployableDeployer.read(builder);
            return builder.build();
        }

        public Run withVariables(Map<String, String> variables) {
            this.variables = this.variables.withAll(variables);
            return this;
        }

        public void apply(Path plan) {
            apply(reader(plan), plan.toString());
        }

        public BufferedReader reader(Path plan) {
            log.info("load config plan from: {}", plan);
            try {
                return Files.newBufferedReader(plan);
            } catch (IOException e) {
                throw new RuntimeException("can't read config plan [" + plan + "]", e);
            }
        }

        public void applyDefaultRoot() {
            log.info("load default root plan");
            apply(new StringReader(DEFAULT_ROOT_BUNDLE), "default root bundle");
        }

        private void apply(Reader reader, String sourceMessage) {
            String failureMessage = "can't apply config plan [" + sourceMessage + "]";
            try {
                this.apply(ConfigurationPlan.load(variables, reader, sourceMessage));
            } catch (WebApplicationApplicationException e) {
                log.info(failureMessage);
                throw e;
            } catch (RuntimeException e) {
                log.debug(failureMessage, e);
                for (Throwable cause = e; cause.getCause() != null; cause = cause.getCause())
                    if (cause.getMessage() != null && !cause.getMessage().isEmpty())
                        failureMessage += ": " + cause.getMessage();
                throw WebException
                        .builderFor(BAD_REQUEST)
                        .causedBy(e)
                        .detail(failureMessage)
                        .build();
            }
        }

        private void apply(ConfigurationPlan plan) {
            // TODO deployers.forEach(deployer -> deployer.apply(plan));
            logHandlerDeployer.apply(plan);
            loggerDeployer.apply(plan);
            deployableDeployer.apply(plan);

            plan.bundles().forEach(this::applyBundle);
        }

        private void applyBundle(BundleConfig bundle) {
            for (Map.Entry<String, Map<String, String>> instance : bundle.getInstances().entrySet()) {
                Variables pop = this.variables;
                try {
                    if (instance.getKey() != null)
                        this.variables = this.variables.with("name", instance.getKey());
                    this.variables = this.variables.withAll(instance.getValue());
                    Artifact artifact = lookup(bundle);
                    apply(artifact.getReader(), artifact.toString());
                } finally {
                    this.variables = pop;
                }
            }
        }
    }

    private Artifact lookup(AbstractArtifactConfig plan) {
        return repository.lookupArtifact(plan.getGroupId(), plan.getArtifactId(), plan.getVersion(),
                plan.getClassifier(), bundle);
    }
}
