package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import org.junit.Test;

import java.io.StringReader;

import static com.github.t1.deployer.container.LogHandlerType.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

public class ConfigurationPlanSerializationTest {
    private static final DeployableConfig FOO = DeployableConfig
            .builder()
            .type(war)
            .name(new DeploymentName("foo"))
            .groupId(new GroupId("org.foo"))
            .artifactId(new ArtifactId("foo-war"))
            .version(new Version("1"))
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
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo-war\n"
            + "    version: 1\n"
            + "    type: war\n";

    private static final ConfigurationPlan ONE_DEPLOYMENT_PLAN = ConfigurationPlan
            .builder()
            .deployable(FOO)
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
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo-war\n"
            + "    version: 1\n"
            + "    type: war\n"
            + "  bar-name:\n"
            + "    group-id: org.bar\n"
            + "    artifact-id: bar-war\n"
            + "    version: 1.2.3\n"
            + "    type: war\n";

    private static final ConfigurationPlan TWO_DEPLOYMENTS_PLAN = ConfigurationPlan
            .builder()
            .deployable(FOO)
            .deployable(DeployableConfig
                    .builder()
                    .type(war)
                    .name(new DeploymentName("bar-name"))
                    .groupId(new GroupId("org.bar"))
                    .artifactId(new ArtifactId("bar-war"))
                    .version(new Version("1.2.3"))
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
            .bundle(BundleConfig
                    .builder()
                    .name(new BundleName("foo"))
                    .groupId(new GroupId("org.foo"))
                    .artifactId(new ArtifactId("foo"))
                    .version(new Version("1"))
                    .variable("name", "bar")
                    .build())
            .build();
    private static final String BUNDLE_PLAN_YAML = ""
            + "bundles:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo\n"
            + "    version: 1\n"
            + "    vars:\n"
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


    private static final String ONE_LOGGER_YAML = ""
            + "loggers:\n"
            + "  some.logger.category:\n"
            + "    level: TRACE\n"
            + "    handlers:\n"
            + "    - CONSOLE\n"
            + "    use-parent-handlers: true\n";
    private static final LoggerConfig LOGGER = LoggerConfig
            .builder()
            .category(LoggerCategory.of("some.logger.category"))
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
            + "  FOO:\n"
            + "    level: INFO\n"
            + "    type: periodic-rotating-file\n"
            + "    format: the-format\n"
            + "    file: the-file\n"
            + "    suffix: the-suffix\n";
    private static final LogHandlerConfig LOGHANDLER = LogHandlerConfig
            .builder()
            .name(new LogHandlerName("FOO"))
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


    private static final String CUSTOM_HANDLER_YAML = ""
            + "log-handlers:\n"
            + "  FOO:\n"
            + "    level: INFO\n"
            + "    type: custom\n"
            + "    format: the-format\n"
            + "    module: org.foo\n"
            + "    class: org.foo.MyHandler\n";
    private static final ConfigurationPlan CUSTOM_HANDLER_PLAN = ConfigurationPlan
            .builder()
            .logHandler(LogHandlerConfig
                    .builder()
                    .name(new LogHandlerName("FOO"))
                    .level(INFO)
                    .type(custom)
                    .format("the-format")
                    .module("org.foo")
                    .class_("org.foo.MyHandler")
                    .build())
            .build();

    @Test
    public void shouldDeserializePlanWithCustomLogHandler() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(CUSTOM_HANDLER_YAML));

        assertThat(plan).isEqualTo(CUSTOM_HANDLER_PLAN);
    }

    @Test
    public void shouldSerializePlanWithCustomLogHandler() throws Exception {
        String yaml = CUSTOM_HANDLER_PLAN.toYaml();

        assertThat(yaml).isEqualTo(CUSTOM_HANDLER_YAML);
    }

    @Test
    public void shouldFailToDeserializePlanWithCustomLogHandlerWithoutModule() throws Exception {
        Throwable thrown = catchThrowable(() -> ConfigurationPlan.load(new StringReader(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: INFO\n"
                + "    type: custom\n"
                + "    format: the-format\n"
                + "    class: org.foo.MyHandler\n")));

        assertThat(thrown.getCause())
                .hasMessageContaining("log-handler [FOO] is of type [custom], so it requires a 'module'");
    }

    @Test
    public void shouldFailToDeserializePlanWithCustomLogHandlerWithoutClass() throws Exception {
        Throwable thrown = catchThrowable(() -> ConfigurationPlan.load(new StringReader(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: INFO\n"
                + "    type: custom\n"
                + "    format: the-format\n"
                + "    module: org.foo\n")));

        assertThat(thrown.getCause())
                .hasMessageContaining("log-handler [FOO] is of type [custom], so it requires a 'class'");
    }
}
