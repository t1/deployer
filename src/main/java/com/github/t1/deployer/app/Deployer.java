package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.Item;
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
    public static final GroupId LOGGERS = new GroupId("loggers");
    public static final GroupId LOG_HANDLERS = new GroupId("log-handlers");

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
            for (GroupId groupId : plan.getGroupIds()) {
                for (ArtifactId artifactId : plan.getArtifactIds(groupId)) {
                    Item item = plan.getItem(groupId, artifactId);

                    log.debug("configure item: {}:{}:{}", groupId, artifactId, item);

                    if (LOGGERS.equals(groupId)) {
                        applyLogger(artifactId, item);
                    } else if (LOG_HANDLERS.equals(groupId)) {
                        applyLogHandler(artifactId, item);
                    } else {
                        applyDeployment(groupId, artifactId, item);
                    }
                }
            }

            if (managed)
                for (Deployment deployment : existing)
                    undeploy(deployment.getName(), repository.getByChecksum(deployment.getChecksum()));
        }

        private void applyLogger(ArtifactId artifactId, Item item) {
            validate(item, logger.class);
            String category = artifactId.toString();
            LoggerResource logger = loggers.logger(category);
            log.debug("check '{}' -> {}", category, item.getState());
            switch (item.getState()) {
            case deployed:
                if (logger.isDeployed()) {
                    if (logger.level().equals(item.getLevel())) {
                        log.info("logger already configured: {}: {}", category, item.getLevel());
                    } else {
                        logger.correctLevel(item.getLevel());
                        audits.add(audit(logger).level(item.getLevel()).updated());
                    }
                } else {
                    logger = logger.toBuilder().level(item.getLevel()).build();
                    logger.add();
                    audits.add(audit(logger).added());
                }
                break;
            case undeployed:
                if (logger.isDeployed()) {
                    logger.remove();
                    audits.add(audit(logger).removed());
                } else {
                    log.info("logger already removed: {}", category);
                }
                break;
            }
        }

        private LoggerAuditBuilder audit(LoggerResource logger) {
            return LoggerAudit.builder().category(logger.category()).level(logger.level());
        }

        private void applyLogHandler(ArtifactId artifactId, Item item) {
            validate(item, loghandler.class);
            String name = artifactId.toString();
            LoggingHandlerType type = item.getHandlerType();
            String file = (item.getFile() == null) ? name : item.getFile();
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

        private void applyDeployment(GroupId groupId, ArtifactId artifactId, Item item) {
            validate(item, deployment.class);
            DeploymentName name = toDeploymentName(item, artifactId);
            log.debug("check '{}' -> {}", name, item.getState());
            Artifact artifact = repository.buildArtifact(groupId, artifactId, item.getVersion(), item.getType());
            switch (item.getState()) {
            case deployed:
                log.debug("found {}:{}:{} => {}", groupId, artifactId, item.getVersion(), artifact);
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

        private DeploymentName toDeploymentName(Item item, ArtifactId artifactId) {
            return new DeploymentName((item.getName() == null) ? artifactId.toString() : item.getName());
        }

        private void deployIf(@NonNull DeploymentName name, @NonNull Artifact artifact) {
            if (artifact.getType() == bundle) {
                this.run(artifact.getReader());
            } else if (existing.removeIf(name::matches)) {
                if (deployments.getDeployment(name).getChecksum().equals(artifact.getChecksum())) {
                    log.info("already deployed with same checksum: {}", name);
                } else {
                    deployments.redeploy(name, artifact.getInputStream());
                    audits.add(audit(artifact).name(name).updated());
                }
            } else {
                deployments.deploy(name, artifact.getInputStream());
                audits.add(audit(artifact).name(name).added());
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
            audits.add(audit(artifact).name(name).removed());
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
