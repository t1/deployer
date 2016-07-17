package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.ArtifactAudit;
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
import java.util.List;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;

@Slf4j
@Singleton
@SuppressWarnings("CdiInjectionPointsInspection")
public class Deployer {
    @Inject ArtifactContainer artifacts;
    @Inject LoggerContainer loggers;
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

    public synchronized Audits run(Reader reader) { return new Run(artifacts.getAllArtifacts()).run(reader); }

    public ConfigurationPlan effectivePlan() {
        ConfigurationPlanBuilder builder = ConfigurationPlan.builder();
        for (Deployment deployment : artifacts.getAllArtifacts()) {
            Artifact artifact = getByChecksum(deployment.getChecksum());
            DeploymentConfig deploymentConfig = DeploymentConfig
                    .builder()
                    .name(deployment.getName())
                    .groupId(artifact.getGroupId())
                    .artifactId(artifact.getArtifactId())
                    .version(artifact.getVersion())
                    .type(artifact.getType())
                    .state(deployed)
                    .build();
            builder.artifact(deployment.getName(), deploymentConfig);
        }
        return builder.build();
    }

    private Artifact getByChecksum(Checksum checksum) {
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
        private final List<Deployment> existing;

        private final LogHandlerDeployer logHandlerDeployer = new LogHandlerDeployer(loggers, audits);
        private final LoggerDeployer loggerDeployer = new LoggerDeployer(loggers, audits);

        private Audits run(Reader reader) {
            this.run(ConfigurationPlan.load(variables.resolve(reader)));
            return audits;
        }

        private void run(ConfigurationPlan plan) {
            plan.logHandlers().forEach(logHandlerDeployer::apply);

            plan.loggers().forEach(loggerDeployer::apply);

            plan.artifacts().forEach(this::applyArtifact);

            if (managed)
                for (Deployment deployment : existing) {
                    DeploymentName name = deployment.getName();
                    artifacts.undeploy(name);
                    audits.audit(ArtifactAudit.of(getByChecksum(deployment.getChecksum())).name(name).removed());
                }
        }

        private void applyArtifact(DeploymentConfig plan) {
            log.debug("apply artifact '{}' -> {}", plan.getName(), plan.getState());
            Artifact artifact = repository
                    .lookupArtifact(plan.getGroupId(), plan.getArtifactId(), plan.getVersion(), plan.getType());
            switch (plan.getState()) {
            case deployed:
                log.debug("found {} => {}", plan, artifact);
                if (artifact.getType() == bundle) {
                    this.run(artifact.getReader());
                } else if (existing.removeIf(plan.getName()::matches)) {
                    if (artifacts.getDeployment(plan.getName()).getChecksum().equals(artifact.getChecksum())) {
                        log.info("already deployed with same checksum: {}", plan.getName());
                    } else {
                        artifacts.redeploy(plan.getName(), artifact.getInputStream());
                        audits.audit(ArtifactAudit.of(artifact).name(plan.getName()).changed());
                    }
                } else {
                    artifacts.deploy(plan.getName(), artifact.getInputStream());
                    audits.audit(ArtifactAudit.of(artifact).name(plan.getName()).added());
                }
                break;
            case undeployed:
                if (existing.removeIf(plan.getName()::matches)) {
                    artifacts.undeploy(plan.getName());
                    audits.audit(ArtifactAudit.of(artifact).name(plan.getName()).removed());
                } else {
                    log.info("already undeployed: {}", plan.getName());
                }
            }
        }
    }
}
