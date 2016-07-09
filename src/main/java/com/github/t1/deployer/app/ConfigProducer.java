package com.github.t1.deployer.app;

import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.RepositoryType;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Produces;
import java.io.*;
import java.net.URI;
import java.nio.file.*;

import static com.github.t1.deployer.app.ConfigurationPlan.*;
import static com.github.t1.deployer.app.DeployerBoundary.*;

@Slf4j
public class ConfigProducer {
    private static final DeployerConfig DEFAULT_CONFIG = DeployerConfig
            .builder()
            .repository(DeployerConfig.RepositoryConfig.builder().build())
            .build();

    @Value
    @Builder
    private static class DeployerConfig {
        private final RepositoryConfig repository;

        @Value
        @Builder
        private static class RepositoryConfig {
            RepositoryType type;
            URI uri;
            String username;
            Password password;
        }
    }


    private DeployerConfig config = DEFAULT_CONFIG;

    public ConfigProducer() {
        Path path = getConfigPath("deployer.config.yaml");
        if (Files.isRegularFile(path)) {
            log.info("load deployer config from '" + path + "'");
            try (Reader reader = Files.newBufferedReader(path)) {
                this.config = YAML.readValue(reader, DeployerConfig.class);
            } catch (IOException e) {
                throw new RuntimeException("can't load config", e);
            }
        } else {
            log.info("no deployer config file at '" + path + "'");
        }
    }

    @Produces @Config("repository.type") public RepositoryType repositoryType() {
        return config.getRepository().getType();
    }

    @Produces @Config("repository.uri") public URI repositoryUri() {
        return config.getRepository().getUri();
    }

    @Produces @Config("repository.username") public String repositoryUsername() {
        return config.getRepository().getUsername();
    }

    @Produces @Config("repository.password") public Password repositoryPassword() {
        return config.getRepository().getPassword();
    }
}
