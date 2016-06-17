package com.github.t1.deployer.app;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@SuppressWarnings("CdiInjectionPointsInspection")
public class Deployer {
    @Inject DeploymentContainer container;
    @Inject Repository repository;

    @Getter @Setter
    private boolean managed;

    public void run(ConfigurationPlan plan) {
        List<Deployment> other = container.getAllDeployments();

        plan.getGroupMap().entrySet().stream().forEach(groupEntry -> {
            GroupId groupId = groupEntry.getKey();
            groupEntry.getValue().entrySet().stream().forEach(artifactEntry -> {
                ArtifactId artifactId = artifactEntry.getKey();
                ConfigurationPlan.Item item = artifactEntry.getValue();

                Artifact artifact = repository.buildArtifact(groupId, artifactId, item.getVersion());
                log.debug("found {}:{}:{} => {}", groupId, artifactId, item.getVersion(), artifact);
                DeploymentName name = new DeploymentName(artifactId.toString());
                switch (item.state) {
                case deployed:
                    if (other.removeIf(name::matches)) {
                        if (container.getDeployment(name).getCheckSum().equals(artifact.getSha1())) {
                            log.info("already deployed with same checksum: {}", name);
                        } else {
                            container.redeploy(name, artifact.getInputStream());
                        }
                    } else {
                        container.deploy(name, artifact.getInputStream());
                    }
                    break;
                case undeployed:
                    if (other.removeIf(name::matches))
                        container.undeploy(name);
                    else
                        log.info("already undeployed: {}", name);
                }
            });
        });

        if (managed)
            other.forEach(deployment -> container.undeploy(deployment.getName()));
    }
}
