package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.log.Logged;
import com.github.t1.problem.WebApplicationApplicationException;
import lombok.SneakyThrows;
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

    @POST
    public Audits post(Map<String, String> form) { return apply(form); }

    @Asynchronous
    @SneakyThrows(InterruptedException.class)
    public void applyAsync() {
        Thread.sleep(1000);
        apply();
    }

    public Audits apply() { return apply(emptyMap()); }


    @Inject Container container;
    @Inject Repository repository;

    @Inject @Config("managed.resources") List<String> managedResourceNames;


    private synchronized Audits apply(Map<String, String> variables) {
        Path root = getConfigPath();

        if (!Files.isRegularFile(root))
            throw new RuntimeException("config file '" + root + "' not found");

        log.debug("load config plan from: {}", root);

        Audits audits = new Run().withVariables(variables).apply(root);

        if (log.isDebugEnabled())
            log.debug("\n{}", audits.toYaml());
        return audits;
    }

    // visible for testing
    Audits apply(String plan) { return new Run().apply(new StringReader(plan)); }

    private class Run {
        private Variables variables = new Variables();
        private final Audits audits = new Audits();

        private final LogHandlerDeployer logHandlerDeployer = new LogHandlerDeployer(container, audits);
        private final LoggerDeployer loggerDeployer = new LoggerDeployer(container, audits);
        private final DeployableDeployer deployableDeployer = new DeployableDeployer(container, audits, repository,
                managed("deployables"), DeployerBoundary.this::lookupByChecksum);

        private boolean managed(String resourceName) {
            return managedResourceNames != null && managedResourceNames.contains(resourceName);
        }

        public ConfigurationPlan read() {
            ConfigurationPlanBuilder builder = ConfigurationPlan.builder();
            logHandlerDeployer.read(builder);
            loggerDeployer.read(builder);
            deployableDeployer.read(builder);
            return builder.build();
        }

        public Run withVariables(Map<String, String> variables) {
            this.variables = this.variables.withAll(variables);
            return this;
        }

        public Audits apply(Path plan) {
            try {
                return apply(Files.newBufferedReader(plan));
            } catch (WebApplicationApplicationException e) {
                log.info("can't apply config plan [{}]", plan);
                throw e;
            } catch (IOException | RuntimeException e) {
                throw new RuntimeException("can't apply config plan [" + plan + "]", e);
            }
        }

        private Audits apply(Reader reader) {
            return this.apply(ConfigurationPlan.load(variables.resolve(reader)));
        }

        private Audits apply(ConfigurationPlan plan) {
            plan.logHandlers().forEach(logHandlerDeployer::apply);
            logHandlerDeployer.cleanup(audits);

            plan.loggers().forEach(loggerDeployer::apply);
            loggerDeployer.cleanup(audits);

            // TODO if we could move this recursion logic into the DeployableDeployer, we could generalize the Deployers
            plan.deployables().forEach(deploymentPlan -> {
                if (deploymentPlan.getType() == bundle)
                    applyBundle(deploymentPlan);
                else
                    deployableDeployer.apply(deploymentPlan);
            });
            deployableDeployer.cleanup(audits);

            return audits;
        }

        private Audits applyBundle(DeployableConfig bundle) {
            Variables pop = this.variables;
            try {
                this.variables = this.variables.withAll(bundle.getVariables());
                return apply(lookup(bundle).getReader());
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


    private Artifact lookup(DeployableConfig deploymentPlan) {
        return repository.lookupArtifact(deploymentPlan.getGroupId(), deploymentPlan.getArtifactId(),
                deploymentPlan.getVersion(), deploymentPlan.getType());
    }

    /** find artifact in repository or return a dummy representing `unknown` or `error`. */
    private Artifact lookupByChecksum(Checksum checksum) {
        if (checksum == null || checksum.isEmpty())
            return errorArtifact(checksum, "empty checksum");
        try {
            return repository.searchByChecksum(checksum);
        } catch (UnknownChecksumException e) {
            return errorArtifact(checksum, "unknown");
        } catch (RuntimeException e) {
            log.error("error retrieving artifact by checksum " + checksum, e);
            return errorArtifact(checksum, "error");
        }
    }

    private Artifact errorArtifact(Checksum checksum, String messageArtifactId) {
        return Artifact
                .builder()
                .groupId(new GroupId("*error*"))
                .artifactId(new ArtifactId(messageArtifactId))
                .version(new Version("unknown"))
                .type(unknown)
                .checksum(checksum)
                .inputStreamSupplier(() -> {
                    throw new UnsupportedOperationException();
                })
                .build();
    }
}
