package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.t1.deployer.tools.Never;
import com.github.t1.meta.GenerateMeta;

import lombok.*;
import lombok.experimental.Accessors;

@Never
@Value
@Builder(builderMethodName = "config")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@Accessors(fluent = true)
@GenerateMeta
public class Config {
    @Value
    @Builder(builderMethodName = "authentication")
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class Authentication {
        @JsonProperty
        public String username;
        @JsonProperty
        public String password;
    }

    @Value
    @Builder(builderMethodName = "repository")
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class RepositoryConfig {
        @JsonProperty
        public URI uri;
        @JsonProperty
        public Authentication authentication;
    }

    @Value
    @Builder(builderMethodName = "container")
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class ContainerConfig {
        @JsonProperty
        public URI uri;
    }

    @JsonProperty
    public RepositoryConfig repository;
    @JsonProperty
    public ContainerConfig container;
}
