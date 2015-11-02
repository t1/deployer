package com.github.t1.deployer.model;

import java.util.Optional;
import java.util.function.Function;

import com.github.t1.deployer.model.Config.*;

import lombok.Value;

@Value
public class ConfigProperties<B> {
    public static ConfigProperties<Config> configProperties() {
        return new ConfigProperties<>(source -> Optional.ofNullable(source));
    }

    Function<B, Optional<Config>> backtrack;

    public DeploymentListFileConfigProperties<B> deploymentListFileConfig() {
        Function<B, Optional<DeploymentListFileConfig>> backtrack =
                source -> this.backtrack.apply(source).map(container -> container.deploymentListFileConfig());
        return new DeploymentListFileConfigProperties<>(backtrack);
    }

    public RepositoryConfigProperties<B> repositoryConfig() {
        Function<B, Optional<RepositoryConfig>> backtrack =
                source -> this.backtrack.apply(source).map(container -> container.repository());
        return new RepositoryConfigProperties<>(backtrack);
    }

    public ContainerConfigProperties<B> containerConfig() {
        Function<B, Optional<ContainerConfig>> backtrack =
                source -> this.backtrack.apply(source).map(container -> container.container());
        return new ContainerConfigProperties<>(backtrack);
    }

    public String $name() {
        return "Configuration";
    }
}
