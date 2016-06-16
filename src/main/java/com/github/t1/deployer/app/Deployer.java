package com.github.t1.deployer.app;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.DeploymentName;
import com.github.t1.deployer.repository.*;

import javax.inject.Inject;

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
                container.deploy(name, artifact.getInputStream());
            });
        });
    }
}
