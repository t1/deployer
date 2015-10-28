package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;
import lombok.experimental.Accessors;

@Value
@Builder(builderMethodName = "config")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@Accessors(fluent = true)
public class Config {
    @Value
    @Builder(builderMethodName = "authentication")
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class Authentication {
        @JsonProperty
        String username;
        @JsonProperty
        String password;
    }

    @Value
    @Builder(builderMethodName = "repository")
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class RepositoryConfig {
        @JsonProperty
        URI uri;
        @JsonProperty
        Authentication authentication;
    }

    @Value
    @Builder(builderMethodName = "container")
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class ContainerConfig {
        @JsonProperty
        URI uri;
    }

    @JsonProperty
    RepositoryConfig repository;
    @JsonProperty
    ContainerConfig container;
}
