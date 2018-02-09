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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static com.github.t1.deployer.app.ConfigProducer.*;
import static com.github.t1.deployer.app.Trigger.*;
import static com.github.t1.deployer.repository.RepositoryType.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.assumeThat;

public class ConfigTest {
    private static final URI DUMMY_URI = URI.create("https://my-artifactory.example.org:9000/artifactory");
    private static final Password SECRET = new Password(UUID.randomUUID().toString());

    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    @Rule public final SystemPropertiesRule systemProperties = new SystemPropertiesRule();
    @SuppressWarnings("resource") @Rule public final FileMemento configFile = new FileMemento(() -> {
        systemProperties.given("jboss.server.config.dir", tmp.getRoot());
        return Paths.get(tmp.getRoot().getPath(), DEPLOYER_CONFIG_YAML);
    });

    private ConfigProducer loadConfig() {
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
    public void shouldConfigureDefaultRepositoryWithoutConfigFile() {
        // given no file

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, null, null, null);
    }

    @Test
    public void shouldConfigureDefaultRepositoryWithEmptyConfigFile() {
        configFile.write("---\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, null, null, null);
    }

    @Test
    public void shouldConfigureDefaultRepositoryWithConfigFileWithoutRepositoryEntry() {
        configFile.write(""
                + "#comment\n"
                + "other: true\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, null, null, null);
    }

    @Test
    public void shouldConfigureDefaultRepositoryWithConfigFileWithEmptyRepositoryEntry() {
        configFile.write(""
                + "repository:\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, null, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithTypeArtifactory() {
        configFile.write(""
                + "repository:\n"
                + "  type: artifactory\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, artifactory, null, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithUri() {
        configFile.write(""
                + "repository:\n"
                + "  uri: " + DUMMY_URI + "\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, null, DUMMY_URI, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithTypeAndUri() {
        configFile.write(""
                + "repository:\n"
                + "  type: artifactory\n"
                + "  uri: " + DUMMY_URI + "\n");

        ConfigProducer producer = loadConfig();

        assertRepository(producer, artifactory, DUMMY_URI, null, null);
    }

    @Test
    public void shouldLoadConfigFileWithTypeAndUriAndCredentials() {
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
    public void shouldLoadConfigFileWithVariable() {
        configFile.write(""
                + "vars:\n"
                + "  foo: bar\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.variables().get(new VariableName("foo"))).isEqualTo("bar");
    }

    @Test
    public void shouldFailToLoadConfigFileWithVariableNameContainingColon() {
        shouldFailToLoadConfigFileWithVariableName("foo:bar");
    }

    @Test
    public void shouldFailToLoadConfigFileWithVariableNameContainingSpace() {
        shouldFailToLoadConfigFileWithVariableName("foo bar");
    }

    private void shouldFailToLoadConfigFileWithVariableName(String variableName) {
        configFile.write(""
                + "vars:\n"
                + "  foo: bar\n"
                + "  " + variableName + ": baz\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.variables()).isEmpty();
    }


    @Test
    public void shouldLoadConfigFileWithDefaultGroupId() {
        configFile.write(""
                + "vars:\n"
                + "  default.group-id: foo\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.variables().get(new VariableName("default.group-id"))).isEqualTo("foo");
    }


    @Test
    public void shouldLoadConfigFileWithManagedDeployables() {
        configFile.write(""
                + "manage:\n"
                + "- deployables\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.managedResources()).containsExactly("deployables");
    }


    @Test
    public void shouldLoadConfigFileWithRootBundleGroupId() {
        configFile.write(""
                + "root-bundle:\n"
                + "  group-id: foo\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getGroupId()).isEqualTo(GroupId.of("foo"));
    }

    @Test
    public void shouldLoadConfigFileWithRootBundleArtifactId() {
        configFile.write(""
                + "root-bundle:\n"
                + "  artifact-id: foo\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getArtifactId()).isEqualTo(new ArtifactId("foo"));
    }

    @Test
    public void shouldLoadConfigFileWithRootBundleClassifier() {
        configFile.write(""
                + "root-bundle:\n"
                + "  classifier: raw\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getClassifier()).isEqualTo(new Classifier("raw"));
    }

    @Test
    public void shouldLoadConfigFileWithRootBundleVersion() {
        configFile.write(""
                + "root-bundle:\n"
                + "  version: 1.0\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getVersion()).isEqualTo(new Version("1.0"));
    }

    @Test
    public void shouldLoadConfigFileWithOnePinnedDeployable() {
        configFile.write(""
                + "pin:\n"
                + "  deployables: [foo]\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.pinned().get("deployables")).containsExactly("foo");
    }

    @Test
    public void shouldLoadConfigFileWithTwoPinnedDeployable() {
        configFile.write(""
                + "pin:\n"
                + "  deployables: [foo, bar]\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.pinned().get("deployables")).containsExactly("foo", "bar");
    }


    @Test
    public void shouldLoadConfigFileWithKeyStore() {
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
    public void shouldLoadConfigFileWithoutKeyStore() {
        configFile.write("");

        ConfigProducer producer = loadConfig();

        assertThat(producer.keyStore()).isNull();
    }

    @Test
    public void shouldLoadConfigFileWithEmptyKeyStore() {
        configFile.write(""
                + "key-store:");

        ConfigProducer producer = loadConfig();

        assertThat(producer.keyStore()).isNull();
    }

    @Test
    public void shouldLoadConfigFileWithEmptyKeyStorePath() {
        configFile.write(""
                + "key-store:\n"
                + "  path:");

        ConfigProducer producer = loadConfig();

        assertThat(producer.keyStore().getPath()).isNull();
    }

    @Test
    public void shouldLoadConfigFileWithOneTrigger() {
        configFile.write(""
                + "triggers: [startup]");

        ConfigProducer producer = loadConfig();

        assertThat(producer.triggers()).containsExactly(startup);
    }

    @Test
    public void shouldLoadConfigFileWithTwoTriggers() {
        configFile.write(""
                + "triggers: [startup, post]");

        ConfigProducer producer = loadConfig();

        assertThat(producer.triggers()).containsExactly(startup, post);
    }

    @Test
    public void shouldLoadConfigFileWithThreeTriggers() {
        configFile.write(""
                + "triggers: [startup, post, fileChange]");

        ConfigProducer producer = loadConfig();

        assertThat(producer.triggers()).containsExactly(startup, post, fileChange);
    }

    @Test
    public void shouldLoadConfigFileWithAllTriggersDefaultToAll() {
        configFile.write("");

        ConfigProducer producer = loadConfig();

        assertThat(producer.triggers()).containsExactly(startup, post, fileChange);
    }

    @Test
    public void shouldFailToGetConfigDirWithoutConfiguredJbossConfigDir() {
        systemProperties.given("jboss.server.config.dir", null);

        Throwable throwable = catchThrowable(Container::getConfigDir);

        assertThat(throwable).hasMessage("no config dir configured");
    }

    @Test
    public void shouldGetConfigDirFromJbossConfigDir() {
        Path configDir = Container.getConfigDir();

        assertThat(configDir).hasToString(System.getProperty("jboss.server.config.dir"));
    }

    @Test
    public void shouldGetConfigDirFromSystemProperty() {
        systemProperties.given("deployer.config.dir", "foobar");

        Path configDir = Container.getConfigDir();

        assertThat(configDir).hasToString("foobar");
    }

    @Test
    public void shouldGetConfigDirFromEnvironmentVariable() {
        assumeThat(System.getenv("DEPLOYER_CONFIG_DIR"))
                .describedAs("set env var 'DEPLOYER_CONFIG_DIR' to 'foo' in run config for this test, but not for others!")
                .isEqualTo("foo");

        Path configDir = Container.getConfigDir();

        assertThat(configDir).hasToString("foo");
    }
}
