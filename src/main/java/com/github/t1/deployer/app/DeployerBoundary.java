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

    public static java.nio.file.Path getConfigPath() { return getConfigPath(ROOT_BUNDLE); }

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
            log.info("skip async run for unresolved variable: {}", e.getVariableName());
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
        Path root = getConfigPath();

        if (!Files.isRegularFile(root))
            throw new RuntimeException("config file '" + root + "' not found");

        log.debug("load config plan from: {}", root);

        new Run().withVariables(variables).apply(root);

        if (log.isDebugEnabled())
            log.debug("\n{}", audits.toYaml());
        return audits;
    }

    // visible for testing
    Audits apply(String plan) {
        new Run().apply(new StringReader(plan));
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
            try {
                apply(Files.newBufferedReader(plan));
            } catch (WebApplicationApplicationException e) {
                log.info("can't apply config plan [{}]", plan);
                throw e;
            } catch (IOException | RuntimeException e) {
                throw new RuntimeException("can't apply config plan [" + plan + "]", e);
            }
        }

        private void apply(Reader reader) { this.apply(ConfigurationPlan.load(variables.resolve(reader))); }

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
                apply(lookup(bundle).getReader());
            } catch (WebApplicationApplicationException e) {
                log.info("can't apply bundle [{}]", bundle);
                throw e;
            } catch (RuntimeException e) {
                throw new RuntimeException("can't apply bundle [" + bundle + "]", e);
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
