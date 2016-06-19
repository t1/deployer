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

    @Inject DeploymentContainer deploymentContainer;
    @Inject LoggerContainer loggerContainer;
    @Inject Repository repository;

    @Getter @Setter
    private boolean managed;

    public void run(String plan) {
        run(ConfigurationPlan.load(plan));
    }

    public void run(ConfigurationPlan plan) {
        List<Deployment> other = deploymentContainer.getAllDeployments();

        plan.getGroupMap().entrySet().stream().forEach(groupEntry -> {
            GroupId groupId = groupEntry.getKey();
            groupEntry.getValue().entrySet().stream().forEach(artifactEntry -> {
                ArtifactId artifactId = artifactEntry.getKey();
                Item item = artifactEntry.getValue();

                if (LOGGERS.equals(groupId)) {
                    applyLogger(artifactId, item);
                } else {
                    applyDeployment(groupId, artifactId, item, other);
                }
            });
        });

        if (managed)
            other.forEach(deployment -> deploymentContainer.undeploy(deployment.getName()));
    }

    private void applyLogger(ArtifactId artifactId, Item item) {
        loggerContainer.add(new LoggerConfig(artifactId.toString(), item.getLevel()));
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
            if (deploymentContainer.getDeployment(name).getCheckSum().equals(artifact.getSha1())) {
                log.info("already deployed with same checksum: {}", name);
            } else {
                deploymentContainer.redeploy(name, artifact.getInputStream());
            }
        } else {
            deploymentContainer.deploy(name, artifact.getInputStream());
        }
    }

    private void undeploy(List<Deployment> other, DeploymentName name) {
        if (other.removeIf(name::matches))
            deploymentContainer.undeploy(name);
        else
            log.info("already undeployed: {}", name);
    }
}
