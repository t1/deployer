package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.Item;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@SuppressWarnings("CdiInjectionPointsInspection")
public class Deployer {
    private static final GroupId LOGGERS = new GroupId("loggers");
    private static final GroupId LOG_HANDLERS = new GroupId("log-handlers");

    @Inject DeploymentContainer deployments;
    @Inject LoggerContainer loggers;
    @Inject Repository repository;

    @Getter @Setter
    private boolean managed;

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
        if (loggers.hasLogger(category)) {
            if (loggers.getLogger(category).getLevel().equals(item.getLevel())) {
                log.info("logger already configured: {}: {}", category, item.getLevel());
            } else {
                loggers.setLogLevel(category, item.getLevel());
            }
        } else {
            loggers.add(new LoggerConfig(category, item.getLevel()));
        }
    }

    private void applyLogHandler(ArtifactId artifactId, Item item) {
        String name = artifactId.toString();
        LoggingHandlerType type = item.getHandlerType();
        loggers.getHandler(type, name)
               .file(item.getFile())
               .suffix(item.getSuffix())
               .formatter(item.getFormatter())
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
        if (other.removeIf(name::matches)) {
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
