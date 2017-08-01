package com.github.t1.deployer.app;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static com.github.t1.deployer.model.Expressions.*;
import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

public class GetVariablesTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .setSerializationInclusion(NON_EMPTY) //
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false) //
            .setPropertyNamingStrategy(KEBAB_CASE);

    private final Path tempDir = AbstractDeployerTests.tempDir();

    @Rule public SystemPropertiesRule systemProperties = new SystemPropertiesRule()
            .given("jboss.server.config.dir", tempDir)
            .given(CLI_DEBUG, "true");

    @Rule public FileMemento configFile = new FileMemento(tempDir.resolve(ROOT_BUNDLE_CONFIG_FILE));

    private DeployerBoundary boundary = new DeployerBoundary();

    private static String toJson(Object object) throws IOException {
        StringWriter out = new StringWriter();
        JSON.writeValue(out, object);
        return out.toString();
    }


    @Test
    public void shouldGetDefaultRootBundle() throws Exception {
        Set<VariableName> variables = boundary.getVariables();

        assertThat(variables).containsExactly(
                new VariableName("default.group-id"),
                new VariableName("version"));
    }

    @Test
    public void shouldGetRootBundleFromConfigFile() throws Exception {
        configFile.write(""
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
