package com.github.t1.deployer.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.ArtifactoryMock.StringInputStream;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.testtools.*;
import org.junit.*;

import java.io.*;
import java.nio.file.Path;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.*;
import static com.github.t1.deployer.app.DeployerBoundary.*;
import static com.github.t1.deployer.container.Container.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.Expressions.*;
import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    DeployerBoundary boundary = new DeployerBoundary();
    Repository repository = mock(Repository.class);

    @Before
    public void setUp() throws Exception {
        boundary.repository = repository;
    }

    private static String toJson(Object object) throws IOException {
        StringWriter out = new StringWriter();
        JSON.writeValue(out, object);
        return out.toString();
    }


    @Test
    public void shouldGetDefaultRootBundleWhenUnresolvedVersion() throws Exception {
        Set<VariableName> variables = boundary.getVariables();

        assertThat(variables).containsExactly(
                new VariableName("default.group-id"),
                new VariableName("version"));
    }

    @Test
    public void shouldGetConfiguredRootBundle() throws Exception {
        boundary.rootBundleConfig = RootBundleConfig
                .builder()
                .groupId(DUMMY_GROUP_ID)
                .artifactId(DUMMY_ARTIFACT_ID)
                .version(DUMMY_VERSION)
                .build();
        when(repository.resolveArtifact(DUMMY_GROUP_ID, DUMMY_ARTIFACT_ID, DUMMY_VERSION, bundle, null))
                .thenReturn(Artifact
                        .builder()
                        .groupId(DUMMY_GROUP_ID)
                        .artifactId(DUMMY_ARTIFACT_ID)
                        .version(DUMMY_VERSION)
                        .type(bundle)
                        .inputStreamSupplier(() -> new StringInputStream(""
                                + "# starting with a comment found a bug\n"
                                + "bundles:\n"
                                + "  app:\n"
                                + "    group-id: com.oneandone.access.apps\n"
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
                        )).build());

        Set<VariableName> variables = boundary.getVariables();

        assertThat(variables).containsExactly(
                new VariableName("jolokia.state"),
                new VariableName("jolokia.version"),
                new VariableName("mockserver.state"),
                new VariableName("mockserver.version"));
    }

    @Test
    public void shouldGetVariablesFromRootBundleFile() throws Exception {
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

    @Test
    public void shouldSerializeBundleTreeAsJson() throws Exception {
        assertThat(toJson(asList(new VariableName("jolokia.state"), new VariableName("jolokia.version"))))
                .isEqualTo("[\"jolokia.state\",\"jolokia.version\"]");
    }
}
