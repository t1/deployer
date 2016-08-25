package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Variables.UnresolvedVariableException;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.log.Logged;
import com.github.t1.problem.WebApplicationApplicationException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.*;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.*;

import static com.github.t1.log.LogLevel.*;
import static java.util.Collections.*;

@javax.ws.rs.Path("/")
@Stateless
@Logged(level = INFO)
@Slf4j
public class DeployerBoundary {
    public static final String ROOT_BUNDLE = "deployer.root.bundle";
    private static final String DEFAULT_ROOT_BUNDLE = ""
            + "bundles:\n"
            + "  ${default.root-bundle-name or hostName()}:\n"
            + "    group-id: ${default.root-bundle-group or default.group-id or domainName()}\n"
            + "    version: ${version}\n";

    public static java.nio.file.Path getRootBundlePath() { return getConfigPath(ROOT_BUNDLE); }

    public static java.nio.file.Path getConfigPath(String fileName) {
        return Paths.get(System.getProperty("jboss.server.config.dir"), fileName);
    }

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
            apply();
        } catch (UnresolvedVariableException e) {
            // not really nice, but seems - over all - better than splitting and repeating the overall control flow
            log.info("skip async run for unresolved variable: {}", e.getExpression());
        }
    }

    public Audits apply() { return apply(emptyMap()); }


    @Inject Container container;
    @Inject Repository repository;

    @Inject Audits audits;
    // TODO @Inject Instance<AbstractDeployer> deployers;
    @Inject DeployableDeployer deployableDeployer;
    @Inject LogHandlerDeployer logHandlerDeployer;
    @Inject LoggerDeployer loggerDeployer;


    private synchronized Audits apply(Map<String, String> variables) {
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

    private class Run {
        private Variables variables = new Variables();

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
            log.info("load config plan from: {}", plan);
            String message = "can't apply config plan [" + plan + "]";
            try {
                BufferedReader reader = Files.newBufferedReader(plan);
                apply(reader, message);
            } catch (IOException e) {
                throw new RuntimeException(message, e);
            }
        }

        public void applyDefaultRoot() {
            log.info("load default root plan");
            apply(new StringReader(DEFAULT_ROOT_BUNDLE), "can't apply default root bundle");
        }

        private void apply(Reader reader, String message) {
            try {
                this.apply(ConfigurationPlan.load(variables.resolve(reader)));
            } catch (WebApplicationApplicationException e) {
                log.info(message);
                throw e;
            } catch (RuntimeException e) {
                throw new RuntimeException(message, e);
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
            Variables pop = this.variables;
            try {
                this.variables = this.variables.withAll(bundle.getVariables());
                apply(lookup(bundle).getReader(), "can't apply bundle [" + bundle + "]");
            } finally {
                this.variables = pop;
            }
        }
    }

    private Artifact lookup(AbstractArtifactConfig plan) {
        return repository.lookupArtifact(plan.getGroupId(), plan.getArtifactId(),
                plan.getVersion(), plan.getType());
    }
}
