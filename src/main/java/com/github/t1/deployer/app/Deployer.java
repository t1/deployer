package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.problem.WebApplicationApplicationException;
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

    public Artifact getByChecksum(Checksum checksum) {
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
        private final Audits audits = auditInstances.get();
        private final List<Deployment> existing;

        private Audits run(Reader reader) {
            this.run(ConfigurationPlan.load(variables.resolve(reader)));
            auditInstances.destroy(audits); // release potential dependent beans
            return audits;
        }

        private void run(ConfigurationPlan plan) {
            plan.logHandlers().forEach(this::applyLogHandler);

            plan.loggers().forEach(this::applyLogger);

            plan.artifacts().forEach(this::applyArtifact);

            if (managed)
                for (Deployment deployment : existing)
                    undeploy(deployment.getName(), getByChecksum(deployment.getChecksum()));
        }

        private void applyLogHandler(LogHandlerConfig plan) {
            LogHandlerName name = plan.getName();
            LoggingHandlerType type = plan.getType();
            LogHandler handler = loggers.handler(type, name);
            log.debug("apply log-handler '{}' -> {}", name, plan.getState());
            switch (plan.getState()) {
            case deployed:
                if (handler.isDeployed())
                    handler.correctLevel(plan.getLevel())
                           .correctFile(plan.getFile())
                           .correctSuffix(plan.getSuffix())
                           .correctFormatter(plan.getFormat(), plan.getFormatter());
                else
                    handler.toBuilder()
                           .file(plan.getFile())
                           .level(plan.getLevel())
                           .suffix(plan.getSuffix())
                           .format(plan.getFormat())
                           .formatter(plan.getFormatter())
                           .build()
                           .add();
                return;
            case undeployed:
                if (handler.isDeployed())
                    handler.remove();
                else
                    log.info("loghandler already removed: {}", name);
                return;
            }
            throw new UnsupportedOperationException("unhandled case: " + plan.getState());
        }

        private void applyLogger(LoggerConfig plan) {
            LoggerResource logger = loggers.logger(plan.getCategory());
            log.debug("apply logger '{}' -> {}", plan.getCategory(), plan.getState());
            switch (plan.getState()) {
            case deployed:
                if (logger.isDeployed()) {
                    int changes = 0;
                    if (!Objects.equals(logger.level(), plan.getLevel())) {
                        logger.writeLevel(plan.getLevel());
                        changes++;
                    }
                    if (!logger.isRoot() && !Objects.equals(logger.useParentHandlers(),
                            nvl(plan.getUseParentHandlers(), true))) {
                        logger.writeUseParentHandlers(plan.getUseParentHandlers());
                        changes++;
                    }
                    if (!Objects.equals(logger.handlers(), plan.getHandlers())) {
                        List<LogHandlerName> existing = new ArrayList<>(logger.handlers());
                        for (LogHandlerName newHandler : plan.getHandlers())
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
                        audits.audit(audit(logger).level(plan.getLevel()).updated());
                    else
                        log.info("logger already configured: {}", plan);
                } else {
                    logger = logger.toBuilder()
                                   .level(plan.getLevel())
                                   .handlers(plan.getHandlers())
                                   .useParentHandlers(plan.getUseParentHandlers())
                                   .build();
                    logger.add();
                    audits.audit(audit(logger).added());
                }
                return;
            case undeployed:
                if (logger.isDeployed()) {
                    logger.remove();
                    audits.audit(audit(logger).removed());
                } else {
                    log.info("logger already removed: {}", plan.getCategory());
                }
                return;
            }
            throw new UnsupportedOperationException("unhandled case: " + plan.getState());
        }

        private LoggerAuditBuilder audit(LoggerResource logger) {
            return LoggerAudit.builder().category(logger.category()).level(logger.level());
        }

        private void applyArtifact(DeploymentConfig plan) {
            log.debug("apply artifact '{}' -> {}", plan.getName(), plan.getState());
            Artifact artifact = repository
                    .lookupArtifact(plan.getGroupId(), plan.getArtifactId(), plan.getVersion(), plan.getType());
            switch (plan.getState()) {
            case deployed:
                log.debug("found {} => {}", plan, artifact);
                deployIf(plan.getName(), artifact);
                break;
            case undeployed:
                undeployIf(plan.getName(), artifact);
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
