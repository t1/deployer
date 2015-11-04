package com.github.t1.deployer.model;

import java.util.Optional;
import java.util.function.Function;

import com.github.t1.deployer.model.Config.*;
import com.github.t1.meta.UriProperty;

import lombok.Value;

@Value
public class Config_RepositoryConfigProperties<B> {
    public static Config_RepositoryConfigProperties<RepositoryConfig> repositoryConfigProperties() {
        return new Config_RepositoryConfigProperties<>(source -> Optional.ofNullable(source));
    }

    Function<B, Optional<RepositoryConfig>> backtrack;

    public UriProperty<B> uri() {
        return new UriProperty<>("uri", "Uri", "The URI where the repository can be reached.",
                source -> this.backtrack.apply(source).map(container -> container.uri()));
    }

    public Config_AuthenticationProperties<B> authentication() {
        Function<B, Optional<Authentication>> backtrack =
                source -> this.backtrack.apply(source).map(container -> container.authentication());
        return new Config_AuthenticationProperties<>(backtrack);
    }

    public String $name() {
        return "Repository Config";
    }
}
