package com.github.t1.deployer.tools;

import static com.github.t1.deployer.model.Config.Authentication.*;
import static com.github.t1.deployer.model.Config.ContainerConfig.*;
import static com.github.t1.deployer.model.Config.RepositoryConfig.*;
import static com.github.t1.deployer.tools.ConfigProducer.*;
import static com.github.t1.rest.fallback.JsonMessageBodyReader.*;
import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;

import org.jboss.as.controller.client.*;
import org.junit.*;

import com.github.t1.deployer.MementoRule;
import com.github.t1.deployer.model.Config;
import com.github.t1.deployer.model.Config.ConfigBuilder;
import com.github.t1.rest.*;

import lombok.SneakyThrows;

public class ConfigTest {
    private static final URI DUMMY_URI = URI.create("https://localhost:1234");


    private final ConfigProducer configProducer = new ConfigProducer();


    @Rule
    public MementoRule<Path> configPath = new MementoRule<>(() -> CONFIG_FILE, (p) -> CONFIG_FILE = p, null);

    @SneakyThrows
    private void givenConfigFile(ConfigBuilder configBuilder) {
        Config config = configBuilder.build();
        CONFIG_FILE = Files.createTempFile("ConfigTest-", ".json");
        MAPPER.writeValue(Files.newBufferedWriter(CONFIG_FILE), config);
    }

    private final Map<String, String> oldSystemProperties = new HashMap<>();

    private void givenSystemProperty(String name, Object value) {
        String previousValue = System.setProperty(name, value.toString());
        oldSystemProperties.put(name, previousValue);
    }

    @After
    public void restoreSystemProperties() {
        for (Entry<String, String> entry : oldSystemProperties.entrySet())
            if (entry.getValue() == null)
                System.clearProperty(entry.getKey());
            else
                System.setProperty(entry.getKey(), entry.getValue());
    }

    @Test
    public void shouldProduceDefaultArtifactoryConfig() {
        RestContext restContext = configProducer.produceArtifactoryRestContext();

        UriTemplate uri = restContext.uri("artifactory");
        assertThat(uri.toString()).isEqualTo("http://localhost:8081/artifactory");
        assertThat(restContext.getCredentials(uri.toUri())).isNull();
    }

    @Test
    public void shouldProduceArtifactoryConfigFromFile() {
        givenConfigFile(Config.config()
                .repository(repository() //
                        .uri(DUMMY_URI)
                        .authentication(authentication() //
                                .username("joe") //
                                .password("doe") //
                                .build()) //
                .build()) //
        );

        RestContext restContext = configProducer.produceArtifactoryRestContext();

        UriTemplate artifactory = restContext.uri("artifactory");
        assertThat(artifactory.toString()).isEqualTo(DUMMY_URI.toString());
        Credentials credentials = restContext.getCredentials(artifactory.toUri());
        assertThat(credentials.userName()).isEqualTo("joe");
        assertThat(credentials.password()).isEqualTo("doe");
    }

    @Test
    public void shouldProduceArtifactoryConfigFromSystemProperty() {
        givenSystemProperty("deployer.artifactory.uri", DUMMY_URI);
        givenSystemProperty("deployer.artifactory.username", "joe");
        givenSystemProperty("deployer.artifactory.password", "doe");

        RestContext restContext = configProducer.produceArtifactoryRestContext();

        UriTemplate artifactory = restContext.uri("artifactory");
        assertThat(artifactory.toString()).isEqualTo(DUMMY_URI.toString());
        assertThat(restContext.getCredentials(artifactory.toUri())).isNull();
    }

    @Test
    public void shouldFailToProduceModelControllerClientWithoutMBeanNorConfig() {
        assertThatThrownBy(() -> configProducer.produceModelControllerClient()) //
                .hasMessageContaining("no config for the container") //
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void shouldProduceModelControllerClientWithConfig() throws Exception {
        givenConfigFile(Config.config().container(container().uri(DUMMY_URI).build()));

        try (ModelControllerClient controllerClient = configProducer.produceModelControllerClient()) {
            assertControllerClient(controllerClient);
        }
    }

    @Test
    public void shouldProduceModelControllerClientWithSystemProperty() throws Exception {
        givenSystemProperty("deployer.container.uri", DUMMY_URI);

        try (ModelControllerClient controllerClient = configProducer.produceModelControllerClient()) {
            assertControllerClient(controllerClient);
        }
    }

    @SneakyThrows
    private void assertControllerClient(ModelControllerClient controllerClient) {
        @SuppressWarnings("resource")
        ModelControllerClientConfiguration clientConfiguration = getClientConfig(controllerClient);

        assertThat(clientConfiguration.getProtocol()).isEqualTo(DUMMY_URI.getScheme());
        assertThat(clientConfiguration.getHost()).isEqualTo(DUMMY_URI.getHost());
        assertThat(clientConfiguration.getPort()).isEqualTo(DUMMY_URI.getPort());
    }

    @SneakyThrows
    private ModelControllerClientConfiguration getClientConfig(ModelControllerClient controllerClient) {
        // the alternatives to mock the factory didn't turn out to be any better
        Field field = controllerClient.getClass().getDeclaredField("clientConfiguration");
        field.setAccessible(true);
        return (ModelControllerClientConfiguration) field.get(controllerClient);
    }
}
