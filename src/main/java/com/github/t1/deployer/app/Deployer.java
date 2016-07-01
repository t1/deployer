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
import javax.validation.*;
import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static javax.ws.rs.core.Response.Status.*;

@Slf4j
@Singleton
@SuppressWarnings("CdiInjectionPointsInspection")
public class Deployer {

    @Inject DeploymentContainer deployments;
    @Inject LoggerContainer loggers;
    @Inject Repository repository;
    @Inject Validator validator;
    @Inject Instance<Audits> auditInstances;

    @Getter @Setter
    private boolean managed; // TODO make configurable for artifacts; add for loggers and handlers (and maybe more)

    @SneakyThrows(IOException.class)
    public Audits run(Path plan) { return run(Files.newBufferedReader(plan)); }

    public Audits run(String plan) { return run(new StringReader(plan)); }

    public synchronized Audits run(Reader reader) { return new Run(deployments.getAllDeployments()).run(reader); }

    @RequiredArgsConstructor
    private class Run {
        private final Variables variables = new Variables();
        private final Audits audits = auditInstances.get();
        private final List<Deployment> existing;

        private Audits run(Reader reader) {
            this.run(ConfigurationPlan.load(variables.resolve(reader)));
            auditInstances.destroy(audits);
            return audits;
        }

        private void run(ConfigurationPlan plan) {
            plan.loggers().forEach(this::applyLogger);

            plan.logHandlers().forEach(this::applyLogHandler);

            plan.deployments().forEach(this::applyDeployment);

            if (managed)
                for (Deployment deployment : existing)
                    undeploy(deployment.getName(), repository.getByChecksum(deployment.getChecksum()));
        }

        private void applyLogger(LoggerConfig item) {
            validate(item, logger.class);
            LoggerResource logger = loggers.logger(item.getCategory());
            log.debug("check '{}' -> {}", item.getCategory(), item.getState());
            switch (item.getState()) {
            case deployed:
                if (logger.isDeployed()) {
                    if (logger.level().equals(item.getLevel())) {
                        log.info("logger already configured: {}: {}", item.getCategory(), item.getLevel());
                    } else {
                        logger.correctLevel(item.getLevel());
                        audits.audit(audit(logger).level(item.getLevel()).updated());
                    }
                } else {
                    logger = logger.toBuilder()
                                   .level(item.getLevel())
                                   .handlers(item.getHandlers())
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
                    log.info("logger already removed: {}", item.getCategory());
                }
                break;
            }
        }

        private LoggerAuditBuilder audit(LoggerResource logger) {
            return LoggerAudit.builder().category(logger.category()).level(logger.level());
        }

        private void applyLogHandler(LogHandlerConfig item) {
            validate(item, loghandler.class);
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

        private void applyDeployment(DeploymentConfig item) {
            validate(item, deployment.class);
            DeploymentName name = item.getDeploymentName();
            log.debug("check '{}' -> {}", name, item.getState());
            Artifact artifact = repository
                    .lookupArtifact(item.getGroupId(), item.getArtifactId(), item.getVersion(), item.getType());
            switch (item.getState()) {
            case deployed:
                log.debug("found {} => {}", item, artifact);
                deployIf(name, artifact);
                break;
            case undeployed:
                undeployIf(name, artifact);
            }
        }

        public <T> void validate(T object, Class<?>... validationGroups) {
            Set<ConstraintViolation<T>> violations = (object == null)
                    ? failValidationNotNull()
                    : validator.validate(object, validationGroups);
            if (violations.isEmpty())
                return;
            log.info("violations found: {}", violations);
            throw new WebApplicationException(Response.status(BAD_REQUEST).entity(violations).build());
        }

        public <T> Set<ConstraintViolation<T>> failValidationNotNull() {
            @Value
            class NotNullContainer {
                @NotNull Object value;
            }
            NotNullContainer notNullContainer = new NotNullContainer(null);
            //noinspection unchecked
            return (Set) validator.validate(notNullContainer);
        }

        private void deployIf(@NonNull DeploymentName name, @NonNull Artifact artifact) {
            if (artifact.getType() == bundle) {
                this.run(artifact.getReader());
            } else if (existing.removeIf(name::matches)) {
                if (deployments.getDeployment(name).getChecksum().equals(artifact.getChecksum())) {
                    log.info("already deployed with same checksum: {}", name);
                } else {
                    deployments.redeploy(name, artifact.getInputStream());
                    audits.audit(audit(artifact).name(name).updated());
                }
            } else {
                deployments.deploy(name, artifact.getInputStream());
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
            deployments.undeploy(name);
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
