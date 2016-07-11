package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Singleton;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.Tools.*;

@Slf4j
@Singleton
@SuppressWarnings("CdiInjectionPointsInspection")
public class Deployer {
    @Inject ArtifactContainer artifacts;
    @Inject LoggerContainer loggers;
    @Inject Repository repository;
    @Inject Instance<Audits> auditInstances;

    @Getter @Setter
    private boolean managed; // TODO make configurable for artifacts; add for loggers and handlers (and maybe more)

    @SneakyThrows(IOException.class)
    public Audits run(Path plan) { return run(Files.newBufferedReader(plan)); }

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

    public Artifact getByChecksum(Checksum checksum) {
        if (checksum == null || checksum.isEmpty())
            return errorArtifact(checksum, "empty checksum");
        try {
            return repository.searchByChecksum(checksum);
        } catch (UnknownChecksumException e) {
            return errorArtifact(checksum, "unknown");
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
        private final Audits audits = auditInstances.get();
        private final List<Deployment> existing;

        private Audits run(Reader reader) {
            this.run(ConfigurationPlan.load(variables.resolve(reader)));
            auditInstances.destroy(audits); // release potential dependent beans
            return audits;
        }

        private void run(ConfigurationPlan plan) {
            plan.loggers().forEach(this::applyLogger);

            plan.logHandlers().forEach(this::applyLogHandler);

            plan.artifacts().forEach(this::applyArtifact);

            if (managed)
                for (Deployment deployment : existing)
                    undeploy(deployment.getName(), getByChecksum(deployment.getChecksum()));
        }

        private void applyLogger(LoggerConfig loggerPlan) {
            LoggerResource logger = loggers.logger(loggerPlan.getCategory());
            log.debug("check '{}' -> {}", loggerPlan.getCategory(), loggerPlan.getState());
            switch (loggerPlan.getState()) {
            case deployed:
                if (logger.isDeployed()) {
                    int changes = 0;
                    if (!Objects.equals(logger.level(), loggerPlan.getLevel())) {
                        logger.writeLevel(loggerPlan.getLevel());
                        changes++;
                    }
                    if (!Objects.equals(logger.useParentHandlers(), nvl(loggerPlan.getUseParentHandlers(), true))) {
                        logger.writeUseParentHandlers(loggerPlan.getUseParentHandlers());
                        changes++;
                    }
                    if (!Objects.equals(logger.handlers(), loggerPlan.getHandlers())) {
                        List<LogHandlerName> existing = new ArrayList<>(logger.handlers());
                        for (LogHandlerName newHandler : loggerPlan.getHandlers())
                            if (!existing.remove(newHandler)) {
                                logger.addLoggerHandler(newHandler);
                                changes++;
                            }
                        for (LogHandlerName oldHandler : existing) {
                            logger.removeLoggerHandler(oldHandler);
                            changes++;
                        }
                    }

                    if (changes > 0)
                        audits.audit(audit(logger).level(loggerPlan.getLevel()).updated());
                    else
                        log.info("logger already configured: {}", loggerPlan);
                } else {
                    logger = logger.toBuilder()
                                   .level(loggerPlan.getLevel())
                                   .handlers(loggerPlan.getHandlers())
                                   .useParentHandlers(loggerPlan.getUseParentHandlers())
                                   .build();
                    logger.add();
                    audits.audit(audit(logger).added());
                }
                break;
            case undeployed:
                if (logger.isDeployed()) {
                    logger.remove();
                    audits.audit(audit(logger).removed());
                } else {
                    log.info("logger already removed: {}", loggerPlan.getCategory());
                }
                break;
            }
        }

        private LoggerAuditBuilder audit(LoggerResource logger) {
            return LoggerAudit.builder().category(logger.category()).level(logger.level());
        }

        private void applyLogHandler(LogHandlerConfig item) {
            LogHandlerName name = item.getName();
            LoggingHandlerType type = item.getType();
            String file = (item.getFile() == null) ? name.getValue() : item.getFile();
            LogHandler handler = loggers.handler(type, name);
            if (handler.isDeployed())
                handler.correctLevel(item.getLevel())
                       .correctFile(file)
                       .correctSuffix(item.getSuffix())
                       .correctFormat(item.getFormat());
            else
                handler.toBuilder()
                       .file(file)
                       .level(item.getLevel())
                       .suffix(item.getSuffix())
                       .format(item.getFormat())
                       .build()
                       .add();
        }

        private void applyArtifact(DeploymentConfig item) {
            log.debug("check '{}' -> {}", item.getName(), item.getState());
            Artifact artifact = repository
                    .lookupArtifact(item.getGroupId(), item.getArtifactId(), item.getVersion(), item.getType());
            switch (item.getState()) {
            case deployed:
                log.debug("found {} => {}", item, artifact);
                deployIf(item.getName(), artifact);
                break;
            case undeployed:
                undeployIf(item.getName(), artifact);
            }
        }

        private void deployIf(@NonNull DeploymentName name, @NonNull Artifact artifact) {
            if (artifact.getType() == bundle) {
                this.run(artifact.getReader());
            } else if (existing.removeIf(name::matches)) {
                if (artifacts.getDeployment(name).getChecksum().equals(artifact.getChecksum())) {
                    log.info("already deployed with same checksum: {}", name);
                } else {
                    artifacts.redeploy(name, artifact.getInputStream());
                    audits.audit(audit(artifact).name(name).updated());
                }
            } else {
                artifacts.deploy(name, artifact.getInputStream());
                audits.audit(audit(artifact).name(name).added());
            }
        }

        private void undeployIf(@NonNull DeploymentName name, @NonNull Artifact artifact) {
            if (existing.removeIf(name::matches)) {
                undeploy(name, artifact);
            } else {
                log.info("already undeployed: {}", name);
            }
        }

        private void undeploy(@NonNull DeploymentName name, @NonNull Artifact artifact) {
            artifacts.undeploy(name);
            audits.audit(audit(artifact).name(name).removed());
        }

        private ArtifactAuditBuilder audit(@NonNull Artifact artifact) {
            return ArtifactAudit
                    .builder()
                    .groupId(artifact.getGroupId())
                    .artifactId(artifact.getArtifactId())
                    .version(artifact.getVersion());
        }
    }
}
