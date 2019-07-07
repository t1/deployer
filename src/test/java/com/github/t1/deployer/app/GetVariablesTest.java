package com.github.t1.deployer.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.RootBundleConfig;
import com.github.t1.deployer.model.Version;
import com.github.t1.deployer.repository.ArtifactoryMock.StringInputStream;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.testtools.FileMemento;
import com.github.t1.testtools.SystemPropertiesRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.KEBAB_CASE;
import static com.github.t1.deployer.app.DeployerBoundary.ROOT_BUNDLE_CONFIG_FILE;
import static com.github.t1.deployer.container.Container.CLI_DEBUG;
import static com.github.t1.deployer.model.ArtifactType.bundle;
import static com.github.t1.deployer.model.Expressions.VariableName;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetVariablesTest {
    private static final ObjectMapper JSON = new ObjectMapper()
        .setSerializationInclusion(NON_EMPTY) //
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false) //
        .setPropertyNamingStrategy(KEBAB_CASE);
    private static final GroupId DUMMY_GROUP_ID = new GroupId("dummy-group");
    private static final ArtifactId DUMMY_ARTIFACT_ID = new ArtifactId("dummy-artifact");
    private static final Version DUMMY_VERSION = new Version("dummy-version");

    private final Path tempDir = AbstractDeployerTests.tempDir();

    @Rule public SystemPropertiesRule systemProperties = new SystemPropertiesRule()
        .given("jboss.server.config.dir", tempDir)
        .given(CLI_DEBUG, "true");

    @Rule public FileMemento rootBundleConfigFile = new FileMemento(tempDir.resolve(ROOT_BUNDLE_CONFIG_FILE));

    private DeployerBoundary boundary = new DeployerBoundary();
    private Repository repository = mock(Repository.class);

    @Before
    public void setUp() { boundary.repository = repository; }

    private static String toJson(Object object) throws IOException {
        StringWriter out = new StringWriter();
        JSON.writeValue(out, object);
        return out.toString();
    }


    @Test public void shouldGetDefaultRootBundleWhenUnresolvedVersion() {
        Set<VariableName> variables = boundary.getVariables();

        assertThat(variables).containsExactly(
            new VariableName("default.group-id"),
            new VariableName("version"));
    }

    @Test public void shouldGetConfiguredRootBundle() {
        boundary.rootBundleConfig = new RootBundleConfig()
            .setGroupId(DUMMY_GROUP_ID)
            .setArtifactId(DUMMY_ARTIFACT_ID)
            .setVersion(DUMMY_VERSION);
        when(repository.resolveArtifact(DUMMY_GROUP_ID, DUMMY_ARTIFACT_ID, DUMMY_VERSION, bundle, null))
            .thenReturn(new Artifact()
                .setGroupId(DUMMY_GROUP_ID)
                .setArtifactId(DUMMY_ARTIFACT_ID)
                .setVersion(DUMMY_VERSION)
                .setType(bundle)
                .setInputStreamSupplier(() -> new StringInputStream(""
                    + "# starting with a comment found a bug\n"
                    + "bundles:\n"
                    + "  app:\n"
                    + "    group-id: com.example.apps\n"
                    + "    version: UNSTABLE # can't use variable here, as it's required in raw format, too\n"
                    + "    instances:\n"
                    + "      jolokia:\n"
                    + "        group-id: org.jolokia\n"
                    + "        artifact-id: jolokia-war\n"
                    + "        version: ${jolokia.version}\n"
                    + "        state: ${jolokia.state or «deployed»}\n"
                    + "      mockserver:\n"
                    + "        group-id: org.mock-server\n"
                    + "        artifact-id: mockserver-war\n"
                    + "        version: ${mockserver.version}\n"
                    + "        state: ${mockserver.state or «deployed»}\n"
                )));

        Set<VariableName> variables = boundary.getVariables();

        assertThat(variables).containsExactly(
            new VariableName("jolokia.state"),
            new VariableName("jolokia.version"),
            new VariableName("mockserver.state"),
            new VariableName("mockserver.version"));
    }

    @Test public void shouldGetVariablesFromRootBundleFile() {
        rootBundleConfigFile.write(""
            + "bundles:\n"
            + "  app:\n"
            + "    group-id: com.github.t1\n"
            + "    version: UNSTABLE\n"
            + "    instances:\n"
            + "      jolokia:\n"
            + "        group-id: org.jolokia\n"
            + "        artifact-id: jolokia-war\n"
            + "        version: ${jolokia.version}\n"
            + "        state: ${jolokia.state or «deployed»}\n"
            + "loggers:\n"
            + "  com.github.t1.deployer:\n"
            + "    level: DEBUG\n");

        Set<VariableName> variables = boundary.getVariables();

        assertThat(variables).containsExactly(
            new VariableName("jolokia.state"),
            new VariableName("jolokia.version"));
    }

    @Test public void shouldSerializeBundleTreeAsJson() throws Exception {
        assertThat(toJson(asList(new VariableName("jolokia.state"), new VariableName("jolokia.version"))))
            .isEqualTo("[\"jolokia.state\",\"jolokia.version\"]");
    }
}
