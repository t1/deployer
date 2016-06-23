package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.Item;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;

import static com.github.t1.deployer.model.ArtifactType.*;

@Slf4j
@SuppressWarnings("CdiInjectionPointsInspection")
public class Deployer {
    private static final GroupId LOGGERS = new GroupId("loggers");
    private static final GroupId LOG_HANDLERS = new GroupId("log-handlers");

    @Inject DeploymentContainer deployments;
    @Inject LoggerContainer loggers;
    @Inject Repository repository;

    @Getter @Setter
    private boolean managed; // TODO make configurable for artifacts, loggers, and handlers (and more in the future)

    public void run(String plan) {
        run(ConfigurationPlan.load(plan));
    }

    public void run(ConfigurationPlan plan) {
        List<Deployment> other = deployments.getAllDeployments();

        plan.getGroupMap().entrySet().stream().forEach(groupEntry -> {
            GroupId groupId = groupEntry.getKey();
            groupEntry.getValue().entrySet().stream().forEach(artifactEntry -> {
                ArtifactId artifactId = artifactEntry.getKey();
                Item item = artifactEntry.getValue();

                if (LOGGERS.equals(groupId)) {
                    applyLogger(artifactId, item);
                } else if (LOG_HANDLERS.equals(groupId)) {
                    applyLogHandler(artifactId, item);
                } else {
                    applyDeployment(groupId, artifactId, item, other);
                }
            });
        });

        if (managed)
            other.forEach(deployment -> deployments.undeploy(deployment.getName()));
    }

    private void applyLogger(ArtifactId artifactId, Item item) {
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
                logger.add();
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

    private void applyLogHandler(ArtifactId artifactId, Item item) {
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

    private void applyDeployment(GroupId groupId, ArtifactId artifactId, Item item,
            List<Deployment> other) {
        DeploymentName name = toDeploymentName(item, artifactId);
        log.debug("check '{}' -> {}", name, item.getState());
        switch (item.getState()) {
        case deployed:
            Artifact artifact = repository.buildArtifact(groupId, artifactId, item.getVersion(),
                    item.getType());
            log.debug("found {}:{}:{} => {}", groupId, artifactId, item.getVersion(), artifact);
            deploy(other, name, artifact);
            break;
        case undeployed:
            undeploy(other, name);
        }
    }

    private DeploymentName toDeploymentName(Item item, ArtifactId artifactId) {
        return new DeploymentName((item.getName() == null) ? artifactId.toString() : item.getName());
    }

    private void deploy(List<Deployment> other, DeploymentName name, Artifact artifact) {
        if (artifact.getType() == bundle) {
            run(ConfigurationPlan.load(artifact.getReader()));
        } else if (other.removeIf(name::matches)) {
            if (deployments.getDeployment(name).getCheckSum().equals(artifact.getSha1())) {
                log.info("already deployed with same checksum: {}", name);
            } else {
                deployments.redeploy(name, artifact.getInputStream());
            }
        } else {
            deployments.deploy(name, artifact.getInputStream());
        }
    }

    private void undeploy(List<Deployment> other, DeploymentName name) {
        if (other.removeIf(name::matches))
            deployments.undeploy(name);
        else
            log.info("already undeployed: {}", name);
    }
}
