package com.github.t1.deployer.app;

import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.Classifier;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.RootBundleConfig;
import com.github.t1.deployer.model.Version;
import com.github.t1.deployer.repository.RepositoryType;
import com.github.t1.deployer.tools.KeyStoreConfig;
import com.github.t1.deployer.tools.Password;
import com.github.t1.testtools.FileMemento;
import com.github.t1.testtools.SystemPropertiesMemento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static com.github.t1.deployer.app.ConfigProducer.DEPLOYER_CONFIG_YAML;
import static com.github.t1.deployer.app.Trigger.fileChange;
import static com.github.t1.deployer.app.Trigger.post;
import static com.github.t1.deployer.app.Trigger.startup;
import static com.github.t1.deployer.repository.RepositoryType.artifactory;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ConfigTest {
    private static final URI DUMMY_URI = URI.create("https://my-artifactory.example.org:9000/artifactory");
    private static final Password SECRET = new Password(UUID.randomUUID().toString());

    @TempDir public Path tmp;
    @RegisterExtension final SystemPropertiesMemento systemProperties = new SystemPropertiesMemento();
    @SuppressWarnings("resource")
    @RegisterExtension final FileMemento configFile = new FileMemento(() -> {
        systemProperties.given("jboss.server.config.dir", tmp);
        return Paths.get(tmp.toString(), DEPLOYER_CONFIG_YAML);
    });

    private ConfigProducer loadConfig() {
        ConfigProducer configProducer = new ConfigProducer();
        configProducer.container = new Container();
        configProducer.initConfig();
        return configProducer;
    }


    @Test void shouldConfigureDefaultRepositoryWithoutConfigFile() {
        // given no file

        ConfigProducer producer = loadConfig();

        assertThat(producer.useDefaultConfig()).isTrue();
        assertThat(producer.repositoryType()).isEqualTo((RepositoryType) null);
        assertThat(producer.repositoryUri()).isEqualTo((URI) null);
        assertThat(producer.repositoryUsername()).isNull();
        assertThat(producer.repositoryPassword()).isNull();
        assertThat(producer.repositorySnapshots()).isNull();
        assertThat(producer.repositoryReleases()).isNull();
    }

    @Test void shouldConfigureDefaultRepositoryWithEmptyConfigFile() {
        configFile.write("---\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.repositoryType()).isEqualTo((RepositoryType) null);
        assertThat(producer.repositoryUri()).isEqualTo((URI) null);
        assertThat(producer.repositoryUsername()).isNull();
        assertThat(producer.repositoryPassword()).isNull();
        assertThat(producer.repositorySnapshots()).isNull();
        assertThat(producer.repositoryReleases()).isNull();
    }

    @Test void shouldConfigureDefaultRepositoryWithConfigFileWithoutRepositoryEntry() {
        configFile.write(""
            + "#comment\n"
            + "other: true\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.repositoryType()).isEqualTo((RepositoryType) null);
        assertThat(producer.repositoryUri()).isEqualTo((URI) null);
        assertThat(producer.repositoryUsername()).isNull();
        assertThat(producer.repositoryPassword()).isNull();
        assertThat(producer.repositorySnapshots()).isNull();
        assertThat(producer.repositoryReleases()).isNull();
    }

    @Test void shouldConfigureDefaultRepositoryWithConfigFileWithEmptyRepositoryEntry() {
        configFile.write(""
            + "repository:\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.repositoryType()).isEqualTo((RepositoryType) null);
        assertThat(producer.repositoryUri()).isEqualTo((URI) null);
        assertThat(producer.repositoryUsername()).isNull();
        assertThat(producer.repositoryPassword()).isNull();
        assertThat(producer.repositorySnapshots()).isNull();
        assertThat(producer.repositoryReleases()).isNull();
    }

    @Test void shouldLoadConfigFileWithTypeArtifactory() {
        configFile.write(""
            + "repository:\n"
            + "  type: artifactory\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.useDefaultConfig()).isFalse();
        assertThat(producer.repositoryType()).isEqualTo(artifactory);
        assertThat(producer.repositoryUri()).isEqualTo((URI) null);
        assertThat(producer.repositoryUsername()).isNull();
        assertThat(producer.repositoryPassword()).isNull();
        assertThat(producer.repositorySnapshots()).isNull();
        assertThat(producer.repositoryReleases()).isNull();
    }

    @Test void shouldLoadConfigFileWithUri() {
        configFile.write(""
            + "repository:\n"
            + "  uri: " + DUMMY_URI + "\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.repositoryType()).isEqualTo((RepositoryType) null);
        assertThat(producer.repositoryUri()).isEqualTo(DUMMY_URI);
        assertThat(producer.repositoryUsername()).isNull();
        assertThat(producer.repositoryPassword()).isNull();
        assertThat(producer.repositorySnapshots()).isNull();
        assertThat(producer.repositoryReleases()).isNull();
    }

    @Test void shouldLoadConfigFileWithTypeAndUri() {
        configFile.write(""
            + "repository:\n"
            + "  type: artifactory\n"
            + "  uri: " + DUMMY_URI + "\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.repositoryType()).isEqualTo(artifactory);
        assertThat(producer.repositoryUri()).isEqualTo(DUMMY_URI);
        assertThat(producer.repositoryUsername()).isNull();
        assertThat(producer.repositoryPassword()).isNull();
        assertThat(producer.repositorySnapshots()).isNull();
        assertThat(producer.repositoryReleases()).isNull();
    }

    @Test void shouldLoadConfigFileWithTypeAndUriAndCredentials() {
        configFile.write(""
            + "repository:\n"
            + "  type: artifactory\n"
            + "  uri: " + DUMMY_URI + "\n"
            + "  username: joe\n"
            + "  password: " + SECRET.getValue() + "\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.repositoryType()).isEqualTo(artifactory);
        assertThat(producer.repositoryUri()).isEqualTo(DUMMY_URI);
        assertThat(producer.repositoryUsername()).isEqualTo("joe");
        assertThat(producer.repositoryPassword()).isEqualTo(SECRET);
        assertThat(producer.repositorySnapshots()).isNull();
        assertThat(producer.repositoryReleases()).isNull();
    }

    @Test void shouldLoadConfigFileWithSnapshots() {
        configFile.write(""
            + "repository:\n"
            + "  snapshots: snap\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.repositoryType()).isNull();
        assertThat(producer.repositoryUri()).isNull();
        assertThat(producer.repositoryUsername()).isNull();
        assertThat(producer.repositoryPassword()).isNull();
        assertThat(producer.repositorySnapshots()).isEqualTo("snap");
        assertThat(producer.repositoryReleases()).isNull();
    }

    @Test void shouldLoadConfigFileWithReleases() {
        configFile.write(""
            + "repository:\n"
            + "  releases: release\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.repositoryType()).isNull();
        assertThat(producer.repositoryUri()).isNull();
        assertThat(producer.repositoryUsername()).isNull();
        assertThat(producer.repositoryPassword()).isNull();
        assertThat(producer.repositorySnapshots()).isNull();
        assertThat(producer.repositoryReleases()).isEqualTo("release");
    }


    @Test void shouldLoadConfigFileWithVariable() {
        configFile.write(""
            + "vars:\n"
            + "  foo: bar\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.variables().get(new VariableName("foo"))).isEqualTo("bar");
    }

    @Test void shouldFailToLoadConfigFileWithVariableNameContainingColon() {
        shouldFailToLoadConfigFileWithVariableName("foo:bar");
    }

    @Test void shouldFailToLoadConfigFileWithVariableNameContainingSpace() {
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


    @Test void shouldLoadConfigFileWithDefaultGroupId() {
        configFile.write(""
            + "vars:\n"
            + "  default.group-id: foo\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.variables().get(new VariableName("default.group-id"))).isEqualTo("foo");
    }


    @Test void shouldLoadConfigFileWithManagedDeployables() {
        configFile.write(""
            + "manage:\n"
            + "- deployables\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.managedResources()).containsExactly("deployables");
    }


    @Test void shouldLoadConfigFileWithRootBundleGroupId() {
        configFile.write(""
            + "root-bundle:\n"
            + "  group-id: foo\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getGroupId()).isEqualTo(GroupId.of("foo"));
    }

    @Test void shouldLoadConfigFileWithRootBundleArtifactId() {
        configFile.write(""
            + "root-bundle:\n"
            + "  artifact-id: foo\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getArtifactId()).isEqualTo(new ArtifactId("foo"));
    }

    @Test void shouldLoadConfigFileWithRootBundleClassifier() {
        configFile.write(""
            + "root-bundle:\n"
            + "  classifier: raw\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getClassifier()).isEqualTo(new Classifier("raw"));
    }

    @Test void shouldLoadConfigFileWithRootBundleVersion() {
        configFile.write(""
            + "root-bundle:\n"
            + "  version: 1.0\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.rootBundle().getVersion()).isEqualTo(new Version("1.0"));
    }

    @Test void shouldLoadConfigFileWithOnePinnedDeployable() {
        configFile.write(""
            + "pin:\n"
            + "  deployables: [foo]\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.pinned().get("deployables")).containsExactly("foo");
    }

    @Test void shouldLoadConfigFileWithTwoPinnedDeployable() {
        configFile.write(""
            + "pin:\n"
            + "  deployables: [foo, bar]\n");

        ConfigProducer producer = loadConfig();

        assertThat(producer.pinned().get("deployables")).containsExactly("foo", "bar");
    }


    @Test void shouldLoadConfigFileWithKeyStore() {
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

    @Test void shouldLoadConfigFileWithoutKeyStore() {
        configFile.write("");

        ConfigProducer producer = loadConfig();

        assertThat(producer.keyStore()).isNull();
    }

    @Test void shouldLoadConfigFileWithEmptyKeyStore() {
        configFile.write(""
            + "key-store:");

        ConfigProducer producer = loadConfig();

        assertThat(producer.keyStore()).isNull();
    }

    @Test void shouldLoadConfigFileWithEmptyKeyStorePath() {
        configFile.write(""
            + "key-store:\n"
            + "  path:");

        ConfigProducer producer = loadConfig();

        assertThat(producer.keyStore().getPath()).isNull();
    }

    @Test void shouldLoadConfigFileWithOneTrigger() {
        configFile.write(""
            + "triggers: [startup]");

        ConfigProducer producer = loadConfig();

        assertThat(producer.triggers()).containsExactly(startup);
    }

    @Test void shouldLoadConfigFileWithTwoTriggers() {
        configFile.write(""
            + "triggers: [startup, post]");

        ConfigProducer producer = loadConfig();

        assertThat(producer.triggers()).containsExactly(startup, post);
    }

    @Test void shouldLoadConfigFileWithThreeTriggers() {
        configFile.write(""
            + "triggers: [startup, post, fileChange]");

        ConfigProducer producer = loadConfig();

        assertThat(producer.triggers()).containsExactly(startup, post, fileChange);
    }

    @Test void shouldLoadConfigFileWithAllTriggersDefaultToAll() {
        configFile.write("");

        ConfigProducer producer = loadConfig();

        assertThat(producer.triggers()).containsExactly(startup, post, fileChange);
    }

    @Test void shouldFailToGetConfigDirWithoutConfiguredJbossConfigDir() {
        systemProperties.given("jboss.server.config.dir", null);

        Throwable throwable = catchThrowable(Container::getConfigDir);

        assertThat(throwable).hasMessage("no config dir configured");
    }

    @Test void shouldGetConfigDirFromJbossConfigDir() {
        Path configDir = Container.getConfigDir();

        assertThat(configDir).hasToString(System.getProperty("jboss.server.config.dir"));
    }

    @Test void shouldGetConfigDirFromSystemProperty() {
        systemProperties.given("deployer.config.dir", "foobar");

        Path configDir = Container.getConfigDir();

        assertThat(configDir).hasToString("foobar");
    }

    @Test void shouldGetConfigDirFromEnvironmentVariable() {
        assumeThat(System.getenv("DEPLOYER_CONFIG_DIR"))
            .describedAs("set env var 'DEPLOYER_CONFIG_DIR' to 'foo' in run config for this test, but not for others!")
            .isEqualTo("foo");

        Path configDir = Container.getConfigDir();

        assertThat(configDir).hasToString("foo");
    }

    @Test void shouldLoadConfigFileWithEverything() {
        String yaml = ""
            + "repository:\n"
            + "  type: artifactory\n"
            + "  uri: " + DUMMY_URI + "\n"
            + "  username: joe\n"
            + "  password: " + SECRET.getValue() + "\n"
            + "  snapshots: snap\n"
            + "  releases: real\n"
            + "root-bundle:\n"
            + "  group-id: foo\n"
            + "  artifact-id: bar\n"
            + "  version: 1.0\n"
            + "  classifier: raw\n"
            + "key-store:\n"
            + "  path: foo\n"
            + "  type: bar\n"
            + "  pass: baz\n"
            + "  alias: bog\n"
            + "vars:\n"
            + "  foo: bar\n"
            + "manage:\n"
            + "- deployables\n"
            + "pin:\n"
            + "  deployables:\n"
            + "  - foo\n"
            + "  - bar\n"
            + "triggers:\n"
            + "- startup\n"
            + "- post\n";
        configFile.write(yaml);

        ConfigProducer producer = loadConfig();

        assertSoftly(softly -> {
            softly.assertThat(producer.repositoryType()).describedAs("repository.type").isEqualTo(artifactory);
            softly.assertThat(producer.repositoryUri()).describedAs("repository.uri").isEqualTo(DUMMY_URI);
            softly.assertThat(producer.repositoryUsername()).describedAs("repository.username").isEqualTo("joe");
            softly.assertThat(producer.repositoryPassword()).describedAs("repository.password").isEqualTo(SECRET);
            softly.assertThat(producer.repositorySnapshots()).describedAs("repository.snapshots").isEqualTo("snap");
            softly.assertThat(producer.repositoryReleases()).describedAs("repository.releases").isEqualTo("real");
            softly.assertThat(producer.variables()).describedAs("vars").containsExactly(entry(new VariableName("foo"), "bar"));
            softly.assertThat(producer.managedResources()).describedAs("managed resource").containsExactly("deployables");
            softly.assertThat(producer.rootBundle()).describedAs("root bundle").isEqualTo(new RootBundleConfig()
                .setGroupId(GroupId.of("foo"))
                .setArtifactId(ArtifactId.of("bar"))
                .setClassifier(Classifier.of("raw"))
                .setVersion(Version.of("1.0")));
            softly.assertThat(producer.pinned()).describedAs("pinned").containsExactly(entry("deployables", asList("foo", "bar")));
            softly.assertThat(producer.keyStore()).describedAs("keystore").isEqualTo(new KeyStoreConfig().withPath("foo").withType("bar").withPass("baz").withAlias("bog"));
            softly.assertThat(producer.triggers()).describedAs("triggers").containsExactly(startup, post);
            softly.assertThat(producer.toString()).describedAs("toString").isEqualTo(yaml.replace(SECRET.getValue(), "concealed"));
        });
    }
}
