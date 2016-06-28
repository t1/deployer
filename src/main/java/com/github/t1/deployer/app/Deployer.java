package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.Item;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Singleton;
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
    @Inject Audits audits;

    @Getter @Setter
    private boolean managed; // TODO make configurable for artifacts; add for loggers and handlers (and maybe more)

    @SneakyThrows(IOException.class)
    public Audits run(Path plan) { return run(Files.newBufferedReader(plan)); }

    public Audits run(String plan) { return run(new StringReader(plan)); }

    public synchronized Audits run(Reader reader) { return new Run(deployments.getAllDeployments()).run(reader); }

    @RequiredArgsConstructor
    private class Run {
        private final Variables variables = new Variables();
        private final List<Deployment> other;

        private Audits run(Reader reader) {
            this.run(ConfigurationPlan.load(variables.resolve(reader)));
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
                other.forEach(deployment -> deployments.undeploy(deployment.getName()));
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
                    }
                } else {
                    logger = logger.toBuilder().level(item.getLevel()).build();
                    logger.add();
                    audits.add(audit(logger).deployed());
                }
                break;
            case undeployed:
                if (logger.isDeployed())
                    logger.remove();
                else
                    log.info("logger already removed: {}", category);
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
            LogHandler handler = loggers.handler(type, name);
            if (handler.isDeployed())
                handler.correctLevel(item.getLevel())
                       .correctFile(item.getFile())
                       .correctSuffix(item.getSuffix())
                       .correctFormatter(item.getFormatter());
            else
                handler.toBuilder()
                       .file(item.getFile())
                       .level(item.getLevel())
                       .suffix(item.getSuffix())
                       .formatter(item.getFormatter())
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
                deploy(name, artifact);
                break;
            case undeployed:
                undeploy(name, artifact);
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

        private void deploy(@NonNull DeploymentName name, @NonNull Artifact artifact) {
            if (artifact.getType() == bundle) {
                this.run(artifact.getReader());
            } else if (other.removeIf(name::matches)) {
                if (deployments.getDeployment(name).getChecksum().equals(artifact.getChecksum())) {
                    log.info("already deployed with same checksum: {}", name);
                } else {
                    deployments.redeploy(name, artifact.getInputStream());
                    audits.add(audit(artifact).deployed());
                }
            } else {
                deployments.deploy(name, artifact.getInputStream());
                audits.add(audit(artifact).name(name).deployed());
            }
        }

        private void undeploy(@NonNull DeploymentName name, @NonNull Artifact artifact) {
            if (other.removeIf(name::matches)) {
                deployments.undeploy(name);
                audits.add(audit(artifact).name(name).undeployed());
            } else {
                log.info("already undeployed: {}", name);
            }
        }

        private ArtifactAudit.ArtifactAuditBuilder audit(@NonNull Artifact artifact) {
            return ArtifactAudit
                    .builder()
                    .groupId(artifact.getGroupId())
                    .artifactId(artifact.getArtifactId())
                    .version(artifact.getVersion());
        }
    }
}
