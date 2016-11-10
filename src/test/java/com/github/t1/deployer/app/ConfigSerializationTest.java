package com.github.t1.deployer.app;

import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.deployer.repository.RepositoryType;
import com.github.t1.deployer.tools.KeyStoreConfig;
import com.github.t1.testtools.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;

import static com.github.t1.deployer.app.ConfigProducer.*;
import static com.github.t1.deployer.repository.RepositoryType.*;
import static org.assertj.core.api.Assertions.*;

public class ConfigSerializationTest {
    private static final URI DUMMY_URI = URI.create("https://my-artifactory.example.org:9000/artifactory");
    private static final Password SECRET = new Password(UUID.randomUUID().toString());

    @Rule public TemporaryFolder folder = new TemporaryFolder();
    @Rule public final SystemPropertiesRule systemProperties = new SystemPropertiesRule();
    @SuppressWarnings("resource") @Rule public final FileMemento configFile = new FileMemento(() -> {
        systemProperties.given("jboss.server.config.dir", folder.getRoot());
        return Paths.get(folder.getRoot().getPath(), DEPLOYER_CONFIG_YAML);
    });

    public ConfigProducer loadConfig() {
        ConfigProducer configProducer = new ConfigProducer();
        configProducer.container = new Container();
        configProducer.initConfig();
        return configProducer;
    }

    private static void assertRepository(ConfigProducer producer, RepositoryType repositoryType, URI uri,
            String username, Password password) {
        assertThat(producer.repositoryType()).isEqualTo(repositoryType);
        assertThat(producer.repositoryUri()).isEqualTo(uri);
        assertThat(producer.repositoryUsername()).isEqualTo(username);
        assertThat(producer.repositoryPassword()).isEqualTo(password);
    }


    @Test
    public void shouldConfigureDefaultRepositoryWithoutConfigFile() throws Exception {
        // given no file

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, null, null, null);
    }

    @Test
    public void shouldConfigureDefaultRepositoryWithEmptyConfigFile() throws Exception {
        configFile.write("---\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, null, null, null);
    }

    @Test
    public void shouldConfigureDefaultRepositoryWithConfigFileWithoutRepositoryEntry() throws Exception {
        configFile.write(""
                + "#comment\n"
                + "other: true\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, null, null, null);
    }

    @Test
    public void shouldConfigureDefaultRepositoryWithConfigFileWithEmptyRepositoryEntry() throws Exception {
        configFile.write(""
                + "repository:\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, null, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithTypeArtifactory() throws Exception {
        configFile.write(""
                + "repository:\n"
                + "  type: artifactory\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, artifactory, null, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithUri() throws Exception {
        configFile.write(""
                + "repository:\n"
                + "  uri: " + DUMMY_URI + "\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, DUMMY_URI, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithTypeAndUri() throws Exception {
        configFile.write(""
                + "repository:\n"
                + "  type: artifactory\n"
                + "  uri: " + DUMMY_URI + "\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, artifactory, DUMMY_URI, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithTypeAndUriAndCredentials() throws Exception {
        configFile.write(""
                + "repository:\n"
                + "  type: artifactory\n"
                + "  uri: " + DUMMY_URI + "\n"
                + "  username: joe\n"
                + "  password: " + SECRET.getValue() + "\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, artifactory, DUMMY_URI, "joe", SECRET);
    }


    @Test
    public void shouldLoadConfigFileWithVariable() throws Exception {
        configFile.write(""
                + "vars:\n"
                + "  foo: bar\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.variables().get(new VariableName("foo"))).isEqualTo("bar");
    }

    @Test
    public void shouldFailToLoadConfigFileWithVariableNameContainingColon() throws Exception {
        shouldFailToLoadConfigFileWithVariableName("foo:bar");
    }

    @Test
    public void shouldFailToLoadConfigFileWithVariableNameContainingSpace() throws Exception {
        shouldFailToLoadConfigFileWithVariableName("foo bar");
    }

    private void shouldFailToLoadConfigFileWithVariableName(String variableName) throws Exception {
        configFile.write(""
                + "vars:\n"
                + "  foo: bar\n"
                + "  " + variableName + ": baz\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.variables()).isEmpty();
    }


    @Test
    public void shouldLoadConfigFileWithDefaultGroupId() throws Exception {
        configFile.write(""
                + "vars:\n"
                + "  default.group-id: foo\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.variables().get(new VariableName("default.group-id"))).isEqualTo("foo");
    }


    @Test
    public void shouldLoadConfigFileWithManagedDeployables() throws Exception {
        configFile.write(""
                + "manage:\n"
                + "- deployables\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.managedResources()).containsExactly("deployables");
    }


    @Test
    public void shouldLoadConfigFileWithRootBundleGroupId() throws Exception {
        configFile.write(""
                + "root-bundle:\n"
                + "  group-id: foo\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getGroupId()).isEqualTo(GroupId.of("foo"));
    }

    @Test
    public void shouldLoadConfigFileWithRootBundleArtifactId() throws Exception {
        configFile.write(""
                + "root-bundle:\n"
                + "  artifact-id: foo\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getArtifactId()).isEqualTo(new ArtifactId("foo"));
    }

    @Test
    public void shouldLoadConfigFileWithRootBundleClassifier() throws Exception {
        configFile.write(""
                + "root-bundle:\n"
                + "  classifier: raw\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getClassifier()).isEqualTo(new Classifier("raw"));
    }

    @Test
    public void shouldLoadConfigFileWithRootBundleVersion() throws Exception {
        configFile.write(""
                + "root-bundle:\n"
                + "  version: 1.0\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getVersion()).isEqualTo(new Version("1.0"));
    }

    @Test
    public void shouldLoadConfigFileWithOnePinnedDeployable() throws Exception {
        configFile.write(""
                + "pin:\n"
                + "  deployables: [foo]\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.pinned().get("deployables")).containsExactly("foo");
    }

    @Test
    public void shouldLoadConfigFileWithTwoPinnedDeployable() throws Exception {
        configFile.write(""
                + "pin:\n"
                + "  deployables: [foo, bar]\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.pinned().get("deployables")).containsExactly("foo", "bar");
    }


    @Test
    public void shouldLoadConfigFileWithKeyStore() throws Exception {
        configFile.write(""
                + "key-store:\n"
                + "  path: foo\n"
                + "  type: bar\n"
                + "  pass: baz\n"
                + "  alias: bog");

        KeyStoreConfig config = loadConfig().keyStore();

        assertThat(config.getPath()).hasToString("foo");
        assertThat(config.getType()).hasToString("bar");
        assertThat(config.getPass()).isEqualTo("baz");
        assertThat(config.getAlias()).isEqualTo("bog");
    }

    @Test
    public void shouldLoadConfigFileWithoutKeyStore() throws Exception {
        configFile.write("");

        ConfigProducer producer = loadConfig();

        assertThat(producer.keyStore()).isNull();
    }

    @Test
    public void shouldLoadConfigFileWithEmptyKeyStore() throws Exception {
        configFile.write(""
                + "key-store:");

        ConfigProducer producer = loadConfig();

        assertThat(producer.keyStore()).isNull();
    }

    @Test
    public void shouldLoadConfigFileWithEmptyKeyStorePath() throws Exception {
        configFile.write(""
                + "key-store:\n"
                + "  path:");

        ConfigProducer producer = loadConfig();

        assertThat(producer.keyStore().getPath()).isNull();
    }
}
