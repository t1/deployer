package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.Config;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.deployer.model.Password;
import com.github.t1.deployer.model.RootBundleConfig;
import com.github.t1.deployer.repository.RepositoryConfig;
import com.github.t1.deployer.repository.RepositoryType;
import com.github.t1.deployer.tools.KeyStoreConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static com.github.t1.deployer.tools.Tools.nvl;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Singleton
public class ConfigProducer {
    private static final ObjectMapper YAML = new ObjectMapper(
        new YAMLFactory()
            .enable(MINIMIZE_QUOTES)
            .disable(WRITE_DOC_START_MARKER))
        .setSerializationInclusion(NON_EMPTY)
        .setPropertyNamingStrategy(new KebabCaseStrategy())
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .findAndRegisterModules();

    static final String DEPLOYER_CONFIG_YAML = "deployer.config.yaml";

    private static final DeployerConfig DEFAULT_CONFIG = new DeployerConfig().setRepository(new RepositoryConfig());

    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor(access = PRIVATE)
    private static class DeployerConfig {
        private RepositoryConfig repository;
        @JsonProperty("root-bundle") private RootBundleConfig rootBundle;
        @JsonProperty("key-store") private KeyStoreConfig keyStore;
        @JsonProperty("vars") private final Map<VariableName, String> variables = new LinkedHashMap<>();
        @JsonProperty("manage") private final List<String> managedResourceNames = new ArrayList<>();
        @JsonProperty("pin") private final Map<String, List<String>> pinned = new LinkedHashMap<>();
        private final EnumSet<Trigger> triggers = EnumSet.allOf(Trigger.class);

        @Override public String toString() { return toYAML(); }

        @SneakyThrows(IOException.class) private String toYAML() { return YAML.writeValueAsString(this); }
    }


    private DeployerConfig config = DEFAULT_CONFIG;

    @Inject Container container;

    @PostConstruct
    public void initConfig() {
        Path path = Container.getConfigDir().resolve(DEPLOYER_CONFIG_YAML);
        if (Files.isRegularFile(path)) {
            log.info("load deployer config from '" + path + "'");
            try (Reader reader = Files.newBufferedReader(path)) {
                DeployerConfig newConfig = YAML.readValue(reader, DeployerConfig.class);
                if (newConfig != null)
                    this.config = newConfig;
            } catch (IOException e) {
                log.error("can't load config from '" + path + "'.\n"
                    + "--------- CONTINUE WITH DEFAULT CONFIG! ---------", e);
            }
        } else {
            log.info("no deployer config file at '" + path + "'; use default config");
        }
    }


    private RepositoryConfig getRepository() { return nvl(config.getRepository(), DEFAULT_CONFIG.getRepository()); }

    @Produces @Config("root-bundle")
    public RootBundleConfig rootBundle() { return config.getRootBundle(); }

    @Produces @Config("key-store")
    public KeyStoreConfig keyStore() { return config.getKeyStore(); }

    @Produces @Config("repository.type")
    public RepositoryType repositoryType() { return getRepository().getType(); }

    @Produces @Config("repository.uri")
    public URI repositoryUri() { return getRepository().getUri(); }

    @Produces @Config("repository.username")
    public String repositoryUsername() { return getRepository().getUsername(); }

    @Produces @Config("repository.password")
    public Password repositoryPassword() { return getRepository().getPassword(); }

    @Produces @Config("repository.snapshots")
    public String repositorySnapshots() { return getRepository().getRepositorySnapshots(); }

    @Produces @Config("repository.releases")
    public String repositoryReleases() { return getRepository().getRepositoryReleases(); }


    @Produces @Config("managed.resources")
    public List<String> managedResources() { return nvl(config.getManagedResourceNames(), emptyList()); }

    @Produces @Config("pinned.resources")
    public Map<String, List<String>> pinned() { return nvl(config.getPinned(), emptyMap()); }

    @Produces @Config("triggers")
    public Set<Trigger> triggers() { return config.getTriggers(); }


    @Produces @Config("variables")
    public Map<VariableName, String> variables() { return config.getVariables(); }

    @Produces @Config("use.default.config") public boolean useDefaultConfig() { return config == DEFAULT_CONFIG; }
}
