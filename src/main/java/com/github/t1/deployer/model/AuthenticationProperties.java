package com.github.t1.deployer.model;

import java.util.Optional;
import java.util.function.Function;

import com.github.t1.deployer.model.Config.Authentication;

import lombok.Value;

@Value
public class AuthenticationProperties<B> {
    public static AuthenticationProperties<Authentication> authenticationProperties() {
        return new AuthenticationProperties<>(source -> Optional.ofNullable(source));
    }

    Function<B, Optional<Authentication>> backtrack;

    public StringProperty<B> username() {
        return new StringProperty<>("username", "User-Name", "The user name used for identify the user.",
                source -> this.backtrack.apply(source).map(container -> container.username()));
    }

    public StringProperty<B> password() {
        return new StringProperty<>("password", "Password", "The secret used to authenticate the user",
                source -> this.backtrack.apply(source).map(container -> container.password()));
    }
}
