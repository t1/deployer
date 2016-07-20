package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
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
import static com.github.t1.deployer.model.DeploymentState.*;

@Slf4j
@Singleton
@SuppressWarnings("CdiInjectionPointsInspection")
public class Deployer {
    @Inject Container container;
    @Inject Repository repository;

    @Getter @Setter
    private boolean managed; // TODO make configurable for artifacts; add for loggers and handlers (and maybe more)

    @SneakyThrows(IOException.class)
    public Audits run(Path plan) {
        try {
            return run(Files.newBufferedReader(plan));
        } catch (WebApplicationApplicationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimeException("can't run config plan [" + plan + "]", e);
        }
    }

    public Audits run(String plan) { return run(new StringReader(plan)); }

    public synchronized Audits run(Reader reader) { return new Run().run(reader); }

    public ConfigurationPlan effectivePlan() {
        ConfigurationPlanBuilder builder = ConfigurationPlan.builder();
        for (DeploymentResource deployment : container.allDeployments()) {
            Artifact artifact = lookupByChecksum(deployment.checksum());
            DeploymentConfig deploymentConfig = DeploymentConfig
                    .builder()
                    .name(deployment.name())
                    .groupId(artifact.getGroupId())
                    .artifactId(artifact.getArtifactId())
                    .version(artifact.getVersion())
                    .type(artifact.getType())
                    .state(deployed)
                    .build();
            builder.artifact(deployment.name(), deploymentConfig);
        }
        return builder.build();
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

    @RequiredArgsConstructor
    private class Run {
        private final Variables variables = new Variables();
        private final Audits audits = new Audits();

        private final LogHandlerDeployer logHandlerDeployer = new LogHandlerDeployer(container, audits);
        private final LoggerDeployer loggerDeployer = new LoggerDeployer(container, audits);
        private final ArtifactDeployer artifactDeployer = new ArtifactDeployer(repository, container, managed, audits,
                Deployer.this::lookupByChecksum);

        private Audits run(Reader reader) {
            this.run(ConfigurationPlan.load(variables.resolve(reader)));
            return audits;
        }

        private void run(ConfigurationPlan plan) {
            plan.logHandlers().forEach(logHandlerDeployer::apply);
            logHandlerDeployer.cleanup(audits);

            plan.loggers().forEach(loggerDeployer::apply);
            loggerDeployer.cleanup(audits);

            plan.artifacts().forEach(deploymentPlan -> {
                if (deploymentPlan.getType() == bundle) {
                    this.run(lookup(deploymentPlan).getReader());
                }
                artifactDeployer.apply(deploymentPlan);
            });
            artifactDeployer.cleanup(audits);
        }
    }
}
