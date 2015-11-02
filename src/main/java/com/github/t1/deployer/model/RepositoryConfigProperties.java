package com.github.t1.deployer.model;

import java.util.Optional;
import java.util.function.Function;

import com.github.t1.deployer.model.Config.*;

import lombok.Value;

@Value
public class RepositoryConfigProperties<B> {
    public static RepositoryConfigProperties<RepositoryConfig> repositoryConfigProperties() {
        return new RepositoryConfigProperties<>(source -> Optional.ofNullable(source));
    }

    Function<B, Optional<RepositoryConfig>> backtrack;

    public UriProperty<B> uri() {
        return new UriProperty<>("uri", "URI", "The URI where the repository can be reached.",
                source -> this.backtrack.apply(source).map(container -> container.uri()));
    }

    public AuthenticationProperties<B> authentication() {
        Function<B, Optional<Authentication>> backtrack =
                source -> this.backtrack.apply(source).map(container -> container.authentication());
        return new AuthenticationProperties<>(backtrack);
    }

    public String $name() {
        return "Repository";
    }
}
