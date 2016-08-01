package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import org.junit.Test;

import java.io.StringReader;

import static com.github.t1.deployer.container.LoggingHandlerType.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

public class ConfigurationPlanSerializationTest {
    private static final DeploymentConfig FOO = DeploymentConfig
            .builder()
            .type(war)
            .name(new DeploymentName("foo"))
            .groupId(new GroupId("org.foo"))
            .artifactId(new ArtifactId("foo-war"))
            .version(new Version("1"))
            .state(deployed)
            .build();


    @Test
    public void shouldSerializeEmptyPlan() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.builder().build();

        String yaml = plan.toYaml();

        assertThat(yaml).isEqualTo("{}\n");
    }

    @Test
    public void shouldDeserializeEmptyPlan() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader("{}"));

        assertThat(plan).isEqualTo(ConfigurationPlan.builder().build());
    }


    private static final String ONE_DEPLOYMENT_YAML = ""
            + "artifacts:\n"
            + "  foo:\n"
            + "    state: deployed\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo-war\n"
            + "    version: 1\n"
            + "    type: war\n";

    private static final ConfigurationPlan ONE_DEPLOYMENT_PLAN = ConfigurationPlan
            .builder()
            .artifact(FOO)
            .build();

    @Test
    public void shouldDeserializePlanWithOneDeployment() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(ONE_DEPLOYMENT_YAML));

        assertThat(plan).isEqualTo(ONE_DEPLOYMENT_PLAN);
    }

    @Test
    public void shouldSerializePlanWithOneDeployment() throws Exception {
        String yaml = ONE_DEPLOYMENT_PLAN.toYaml();

        assertThat(yaml).isEqualTo(ONE_DEPLOYMENT_YAML);
    }


    private static final String TWO_DEPLOYMENTS_YAML = ""
            + "artifacts:\n"
            + "  foo:\n"
            + "    state: deployed\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo-war\n"
            + "    version: 1\n"
            + "    type: war\n"
            + "  bar-name:\n"
            + "    state: deployed\n"
            + "    group-id: org.bar\n"
            + "    artifact-id: bar-war\n"
            + "    version: 1.2.3\n"
            + "    type: war\n";

    private static final ConfigurationPlan TWO_DEPLOYMENTS_PLAN = ConfigurationPlan
            .builder()
            .artifact(FOO)
            .artifact(DeploymentConfig
                    .builder()
                    .type(war)
                    .name(new DeploymentName("bar-name"))
                    .groupId(new GroupId("org.bar"))
                    .artifactId(new ArtifactId("bar-war"))
                    .version(new Version("1.2.3"))
                    .state(deployed)
                    .build())
            .build();

    @Test
    public void shouldDeserializePlanWithTwoDeployments() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(TWO_DEPLOYMENTS_YAML));

        assertThat(plan).isEqualTo(TWO_DEPLOYMENTS_PLAN);
    }

    @Test
    public void shouldSerializePlanWithTwoDeployments() throws Exception {
        String yaml = TWO_DEPLOYMENTS_PLAN.toYaml();

        assertThat(yaml).isEqualTo(TWO_DEPLOYMENTS_YAML);
    }


    private static final ConfigurationPlan BUNDLE_PLAN = ConfigurationPlan
            .builder()
            .artifact(DeploymentConfig
                    .builder()
                    .type(bundle)
                    .name(new DeploymentName("foo"))
                    .groupId(new GroupId("org.foo"))
                    .artifactId(new ArtifactId("foo"))
                    .version(new Version("1"))
                    .variable("name", "bar")
                    .state(deployed)
                    .build())
            .build();
    private static final String BUNDLE_PLAN_YAML = ""
            + "artifacts:\n"
            + "  foo:\n"
            + "    state: deployed\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo\n"
            + "    version: 1\n"
            + "    type: bundle\n"
            + "    var:\n"
            + "      name: bar\n";

    @Test
    public void shouldDeserializePlanWithBundleDeploymentWithVars() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(BUNDLE_PLAN_YAML));

        assertThat(plan).isEqualTo(BUNDLE_PLAN);
    }

    @Test
    public void shouldSerializePlanWithBundleDeploymentWithVars() throws Exception {
        String yaml = BUNDLE_PLAN.toYaml();

        assertThat(yaml).isEqualTo(BUNDLE_PLAN_YAML);
    }


    @Test
    public void shouldFailToDeserializePlanWithWarDeploymentWithVars() throws Exception {
        Throwable thrown = catchThrowable(() -> ConfigurationPlan.load(new StringReader(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1\n"
                + "    var:\n"
                + "      name: bar\n")));

        assertThat(thrown).hasMessageContaining("exception while loading config plan");
        assertThat(thrown.getCause()).hasMessageContaining("Instantiation of ").hasMessageContaining(" value failed");
        assertThat(thrown.getCause().getCause())
                .isInstanceOf(ConfigurationPlanLoadingException.class)
                .hasMessageContaining("variables are only allowed for bundles");
    }


    private static final String ONE_LOGGER_YAML = ""
            + "loggers:\n"
            + "  some.logger.category:\n"
            + "    state: deployed\n"
            + "    level: TRACE\n"
            + "    handlers:\n"
            + "    - CONSOLE\n"
            + "    use-parent-handlers: true\n";
    private static final LoggerConfig LOGGER = LoggerConfig
            .builder()
            .category(LoggerCategory.of("some.logger.category"))
            .state(deployed)
            .level(TRACE)
            .handler("CONSOLE")
            .useParentHandlers(true)
            .build();
    private static final ConfigurationPlan ONE_LOGGER_PLAN = ConfigurationPlan
            .builder()
            .logger(LOGGER)
            .build();

    @Test
    public void shouldDeserializePlanWithOneLogger() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(ONE_LOGGER_YAML));

        assertThat(plan).isEqualTo(ONE_LOGGER_PLAN);
    }

    @Test
    public void shouldSerializePlanWithOneLogger() throws Exception {
        String yaml = ONE_LOGGER_PLAN.toYaml();

        assertThat(yaml).isEqualTo(ONE_LOGGER_YAML);
    }


    private static final String ONE_LOGHANDLER_YAML = ""
            + "log-handlers:\n"
            + "  CONSOLE:\n"
            + "    state: deployed\n"
            + "    level: INFO\n"
            + "    type: periodicRotatingFile\n"
            + "    format: the-format\n"
            + "    file: the-file\n"
            + "    suffix: the-suffix\n";
    private static final LogHandlerConfig LOGHANDLER = LogHandlerConfig
            .builder()
            .name(new LogHandlerName("CONSOLE"))
            .state(deployed)
            .level(INFO)
            .type(periodicRotatingFile)
            .file("the-file")
            .suffix("the-suffix")
            .format("the-format")
            .build();
    private static final ConfigurationPlan ONE_LOGHANDLER_PLAN = ConfigurationPlan
            .builder()
            .logHandler(LOGHANDLER)
            .build();

    @Test
    public void shouldDeserializePlanWithOneLogHandler() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(ONE_LOGHANDLER_YAML));

        assertThat(plan).isEqualTo(ONE_LOGHANDLER_PLAN);
    }

    @Test
    public void shouldSerializePlanWithOneLogHandler() throws Exception {
        String yaml = ONE_LOGHANDLER_PLAN.toYaml();

        assertThat(yaml).isEqualTo(ONE_LOGHANDLER_YAML);
    }
}
