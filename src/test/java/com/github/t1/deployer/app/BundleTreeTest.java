package com.github.t1.deployer.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.t1.deployer.model.BundleTree;
import com.github.t1.deployer.model.BundleTree.Variable;
import com.github.t1.testtools.*;
import org.junit.*;

import java.io.*;
import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.*;
import static com.github.t1.deployer.app.DeployerBoundary.*;
import static com.github.t1.deployer.container.Container.*;
import static com.github.t1.deployer.model.Expressions.*;
import static org.assertj.core.api.Assertions.*;

public class BundleTreeTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .setSerializationInclusion(NON_EMPTY) //
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false) //
            .setPropertyNamingStrategy(KEBAB_CASE);

    private static final BundleTree FULL_TREE = BundleTree
            .builder()
            .variable(Variable.builder().name("jolokia.version").mandatory(false).build())
            // TODO .variable(Variable.builder().name("jolokia.state").mandatory(true).build()) instead of:
            .variable(Variable.builder().name("jolokia.state or «deployed»").build())
            .bundle(BundleTree.builder()
                              .name("app")
                              .groupId("com.github.t1")
                              .artifactId("app")
                              .version("UNSTABLE")
                              // TODO jolokia .bundle()
                              .build())
            .build();

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
        BundleTree tree = boundary.getBundles();

        assertThat(tree).isEqualTo(BundleTree
                .builder()
                .variable(Variable.builder().name("regex(root-bundle:artifact-id or hostName(), «(.*?)\\d*»)").build())
                .variable(Variable.builder().name("root-bundle:version or version").build())
                .variable(Variable.builder().name("root-bundle:classifier or null").build())
                .bundle(BundleTree
                        .builder()
                        .name("${regex(root-bundle:artifact-id or hostName(), «(.*?)\\d*»)}")
                        .groupId(domainName())
                        .artifactId("${regex(root-bundle:artifact-id or hostName(), «(.*?)\\d*»)}")
                        .version("${root-bundle:version or version}")
                        .classifier("${root-bundle:classifier or null}")
                        .build())
                .build());
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

        BundleTree tree = boundary.getBundles();

        assertThat(tree).isEqualTo(FULL_TREE);
    }

    @Test
    public void shouldSerializeBundleTreeAsJson() throws Exception {
        assertThat(toJson(FULL_TREE)).isEqualTo(""
                + "{\"variables\":["
                + /**/"{\"name\":\"jolokia.version\",\"mandatory\":false},"
                + /**/"{\"name\":\"jolokia.state or «deployed»\",\"mandatory\":false}"
                + "],"
                + "\"bundles\":["
                + /**/"{\"name\":\"app\",\"group-id\":\"com.github.t1\",\"artifact-id\":\"app\",\"version\":\"UNSTABLE\"}"
                + "]}");
    }
}
