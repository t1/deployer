package com.github.t1.deployer.model;

import java.util.Optional;
import java.util.function.Function;

import com.github.t1.deployer.model.Config.DeploymentListFileConfig;

import lombok.Value;

@Value
public class DeploymentListFileConfigProperties<B> {
    public static DeploymentListFileConfigProperties<DeploymentListFileConfig> deploymentListFileConfigProperties() {
        return new DeploymentListFileConfigProperties<>(source -> Optional.ofNullable(source));
    }

    Function<B, Optional<DeploymentListFileConfig>> backtrack;

    public BooleanProperty<B> autoUndeploy() {
        return new BooleanProperty<>("autoUndeploy", "Auto-Undeploy",
                "Automatically delete all deployments not found in the deployments list.",
                source -> this.backtrack.apply(source).map(container -> container.autoUndeploy()));
    }

    public String $name() {
        return "Deployment-List-File";
    }
}
