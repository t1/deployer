package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Password;
import com.github.t1.deployer.repository.RepositoryType;
import com.github.t1.testtools.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;

import static com.github.t1.deployer.app.ConfigProducer.*;
import static com.github.t1.deployer.repository.RepositoryType.*;
import static org.assertj.core.api.Assertions.*;

public class DeployerConfigSerializationTest {
    private static final URI DUMMY_URI = URI.create("https://my-artifactory.example.org:9000/artifactory");
    private static final Password SECRET = new Password(UUID.randomUUID().toString());

    @Rule public TemporaryFolder folder = new TemporaryFolder();
    @Rule public final SystemPropertiesRule systemProperties = new SystemPropertiesRule();
    @Rule public final FileMemento configFile = new FileMemento(() -> {
        systemProperties.given("jboss.server.config.dir", folder.getRoot());
        return Paths.get(folder.getRoot().getPath(), DEPLOYER_CONFIG_YAML);
    });

    private static void assertConfig(ConfigProducer producer, RepositoryType repositoryType, URI uri,
            String username, Password password) {
        assertThat(producer.repositoryType()).isEqualTo(repositoryType);
        assertThat(producer.repositoryUri()).isEqualTo(uri);
        assertThat(producer.repositoryUsername()).isEqualTo(username);
        assertThat(producer.repositoryPassword()).isEqualTo(password);
    }

    @Test
    public void shouldReturnToDefaultWithoutConfigFile() throws Exception {
        // given no file

        ConfigProducer producer = new ConfigProducer();

        assertConfig(producer, null, null, null, null);
    }

    @Test
    public void shouldReturnToDefaultWithEmptyConfigFile() throws Exception {
        configFile.write("");

        ConfigProducer producer = new ConfigProducer();

        assertConfig(producer, null, null, null, null);
    }

    @Test
    public void shouldReturnToDefaultWithConfigFileWithoutRepositoryEntry() throws Exception {
        configFile.write(""
                + "#comment\n"
                + "other: true\n");

        ConfigProducer producer = new ConfigProducer();

        assertConfig(producer, null, null, null, null);
    }

    @Test
    public void shouldReturnToDefaultWithConfigFileWithEmptyRepositoryEntry() throws Exception {
        configFile.write(""
                + "repository:\n");

        ConfigProducer producer = new ConfigProducer();

        assertConfig(producer, null, null, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithTypeArtifactory() throws Exception {
        configFile.write(""
                + "repository:\n"
                + "  type: artifactory\n");

        ConfigProducer producer = new ConfigProducer();

        assertConfig(producer, artifactory, null, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithUri() throws Exception {
        configFile.write(""
                + "repository:\n"
                + "  uri: " + DUMMY_URI + "\n");

        ConfigProducer producer = new ConfigProducer();

        assertConfig(producer, null, DUMMY_URI, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithTypeAndUri() throws Exception {
        configFile.write(""
                + "repository:\n"
                + "  type: artifactory\n"
                + "  uri: " + DUMMY_URI + "\n");

        ConfigProducer producer = new ConfigProducer();

        assertConfig(producer, artifactory, DUMMY_URI, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithTypeAndUriAndCredentials() throws Exception {
        configFile.write(""
                + "repository:\n"
                + "  type: artifactory\n"
                + "  uri: " + DUMMY_URI + "\n"
                + "  username: joe\n"
                + "  password: " + SECRET.getValue() + "\n");

        ConfigProducer producer = new ConfigProducer();

        assertConfig(producer, artifactory, DUMMY_URI, "joe", SECRET);
    }
}
