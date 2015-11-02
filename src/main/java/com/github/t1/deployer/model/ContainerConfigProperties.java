package com.github.t1.deployer.model;

import java.util.Optional;
import java.util.function.Function;

import com.github.t1.deployer.model.Config.ContainerConfig;

import lombok.Value;

@Value
public class ContainerConfigProperties<B> {
    public static ContainerConfigProperties<ContainerConfig> containerConfigProperties() {
        return new ContainerConfigProperties<>(source -> Optional.ofNullable(source));
    }

    Function<B, Optional<ContainerConfig>> backtrack;

    public UriProperty<B> uri() {
        return new UriProperty<>("uri", "URI", "The URI where the container can be reached.",
                source -> this.backtrack.apply(source).map(container -> container.uri()));
    }

    public String $name() {
        return "Container";
    }
}
