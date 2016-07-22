package com.github.t1.deployer.app;

import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.RepositoryType;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.io.*;
import java.net.URI;
import java.nio.file.*;

import static com.github.t1.deployer.app.ConfigurationPlan.*;
import static com.github.t1.deployer.app.DeployerBoundary.*;
import static com.github.t1.deployer.model.Tools.*;
import static lombok.AccessLevel.*;

@Slf4j
@Singleton
public class ConfigProducer {
    public static final String DEPLOYER_CONFIG_YAML = "deployer.config.yaml";

    private static final DeployerConfig DEFAULT_CONFIG = DeployerConfig
            .builder()
            .repository(DeployerConfig.RepositoryConfig.builder().build())
            .build();

    @Value
    @Builder
    @NoArgsConstructor(access = PRIVATE, force = true)
    @AllArgsConstructor(access = PRIVATE)
    private static class DeployerConfig {
        private final RepositoryConfig repository;

        @Value
        @Builder
        @NoArgsConstructor(access = PRIVATE, force = true)
        @AllArgsConstructor(access = PRIVATE)
        private static class RepositoryConfig {
            RepositoryType type;
            URI uri;
            String username;
            Password password;
            String key;
        }

        @Override public String toString() { return toYAML(); }

        @SneakyThrows(IOException.class) private String toYAML() { return YAML.writeValueAsString(this); }
    }


    private DeployerConfig config = DEFAULT_CONFIG;

    public ConfigProducer() {
        Path path = getConfigPath(DEPLOYER_CONFIG_YAML);
        if (Files.isRegularFile(path)) {
            log.info("load deployer config from '" + path + "'");
            try (Reader reader = Files.newBufferedReader(path)) {
                this.config = nvl(YAML.readValue(reader, DeployerConfig.class), this.config);
            } catch (IOException e) {
                log.error("can't load config from '" + path + "'.\n"
                        + "--------- CONTINUE WITH DEFAULT CONFIG! ---------\n", e);
            }
        } else {
            log.info("no deployer config file at '" + path + "'");
        }
    }

    public DeployerConfig.RepositoryConfig getRepository() {
        return nvl(config.getRepository(), DEFAULT_CONFIG.getRepository());
    }

    @Produces @Config("repository.type")
    public RepositoryType repositoryType() { return getRepository().getType(); }

    @Produces @Config("repository.uri")
    public URI repositoryUri() {
        return getRepository().getUri();
    }

    @Produces @Config("repository.username")
    public String repositoryUsername() { return getRepository().getUsername(); }

    @Produces @Config("repository.password")
    public Password repositoryPassword() { return getRepository().getPassword(); }

    @Produces @Config("repository.key")
    public String repositoryKey() { return getRepository().getKey(); }
}
