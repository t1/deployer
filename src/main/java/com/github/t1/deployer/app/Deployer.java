package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.problem.WebApplicationApplicationException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.io.*;
import java.nio.file.*;

import static com.github.t1.deployer.model.ArtifactType.*;

@Slf4j
@Singleton
@SuppressWarnings("CdiInjectionPointsInspection")
public class Deployer {
    @Inject Container container;
    @Inject Repository repository;

    @Getter @Setter
    private boolean managed; // TODO make configurable for artifacts; add for loggers and handlers (and maybe more)


    public ConfigurationPlan effectivePlan() { return new Run().read(); }


    @SneakyThrows(IOException.class)
    public Audits apply(Path plan) {
        try {
            return apply(Files.newBufferedReader(plan));
        } catch (WebApplicationApplicationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimeException("can't run config plan [" + plan + "]", e);
        }
    }

    public Audits apply(String plan) { return apply(new StringReader(plan)); }

    public synchronized Audits apply(Reader reader) { return new Run().apply(reader); }

    private class Run {
        private final Variables variables = new Variables();
        private final Audits audits = new Audits();

        private final LogHandlerDeployer logHandlerDeployer = new LogHandlerDeployer(container, audits);
        private final LoggerDeployer loggerDeployer = new LoggerDeployer(container, audits);
        private final ArtifactDeployer artifactDeployer = new ArtifactDeployer(repository, container, managed, audits,
                Deployer.this::lookupByChecksum);

        public ConfigurationPlan read() {
            ConfigurationPlanBuilder builder = ConfigurationPlan.builder();
            loggerDeployer.read(builder);
            logHandlerDeployer.read(builder);
            artifactDeployer.read(builder);
            return builder.build();
        }

        private Audits apply(Reader reader) {
            this.apply(ConfigurationPlan.load(variables.resolve(reader)));
            return audits;
        }

        private void apply(ConfigurationPlan plan) {
            plan.logHandlers().forEach(logHandlerDeployer::apply);
            logHandlerDeployer.cleanup(audits);

            plan.loggers().forEach(loggerDeployer::apply);
            loggerDeployer.cleanup(audits);

            // TODO if we could move this recursion logic into the ArtifactDeployer, we could generalize the Deployers
            plan.artifacts().forEach(deploymentPlan -> {
                if (deploymentPlan.getType() == bundle) {
                    this.apply(lookup(deploymentPlan).getReader());
                }
                artifactDeployer.apply(deploymentPlan);
            });
            artifactDeployer.cleanup(audits);
        }
    }


    private Artifact lookup(DeploymentConfig deploymentPlan) {
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
