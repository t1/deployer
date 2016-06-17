package com.github.t1.deployer.app;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.DeploymentName;
import com.github.t1.deployer.repository.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class Deployer {
    @Inject DeploymentContainer container;
    @Inject Repository repository;

    public void run(ConfigurationPlan plan) {
        plan.getGroupMap().entrySet().stream().forEach(groupEntry -> {
            GroupId groupId = groupEntry.getKey();
            groupEntry.getValue().entrySet().stream().forEach(artifactEntry -> {
                ArtifactId artifactId = artifactEntry.getKey();
                ConfigurationPlan.Item item = artifactEntry.getValue();
                Artifact artifact = repository.fetchArtifact(groupId, artifactId, item.getVersion());
                DeploymentName name = new DeploymentName(artifactId.toString());
                if (container.hasDeployment(name)) {
                    if (container.getDeployment(name).getCheckSum().equals(artifact.getSha1())) {
                        log.info("already deployed with same checksum: {}", name);
                    } else {
                        container.redeploy(name, artifact.getInputStream());
                    }
                } else {
                    container.deploy(name, artifact.getInputStream());
                }
            });
        });
    }
}
