package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Plan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.io.*;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.container.LogHandlerType.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PlanSerializationTest {
    private static final DeployablePlan FOO = DeployablePlan
            .builder()
            .type(war)
            .name(new DeploymentName("foo"))
            .groupId(new GroupId("org.foo"))
            .artifactId(new ArtifactId("foo-war"))
            .version(new Version("1"))
            .build();
    private static Variables variables = mock(Variables.class);

    static {
        when(variables.resolve(any(Reader.class))).then(invocation -> invocation.getArgument(0));
        when(variables.resolve(anyString())).then(i -> new Variables.Resolver() {});
    }


    @Test
    public void shouldSerializeEmptyPlan() throws Exception {
        Plan plan = Plan.builder().build();

        String yaml = plan.toYaml();

        assertThat(yaml).isEqualTo("{}\n");
    }

    @Test
    public void shouldDeserializeEmptyPlan() throws Exception {
        Plan plan = Plan.load(variables, new StringReader("{}"), "empty");

        assertThat(plan).isEqualTo(Plan.builder().build());
    }


    private static final String ONE_DEPLOYMENT_YAML = ""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo-war\n"
            + "    version: 1\n"
            + "    type: war\n";

    private static final Plan ONE_DEPLOYMENT_PLAN = Plan
            .builder()
            .deployable(FOO)
            .build();

    @Test
    public void shouldDeserializePlanWithOneDeployment() throws Exception {
        Plan plan = Plan.load(variables, new StringReader(ONE_DEPLOYMENT_YAML), "yaml1");

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

    private static final Plan TWO_DEPLOYMENTS_PLAN = Plan
            .builder()
            .deployable(FOO)
            .deployable(DeployablePlan
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
        Plan plan = Plan.load(variables, new StringReader(TWO_DEPLOYMENTS_YAML), "yaml2");

        assertThat(plan).isEqualTo(TWO_DEPLOYMENTS_PLAN);
    }

    @Test
    public void shouldSerializePlanWithTwoDeployments() throws Exception {
        String yaml = TWO_DEPLOYMENTS_PLAN.toYaml();

        assertThat(yaml).isEqualTo(TWO_DEPLOYMENTS_YAML);
    }


    private static final Plan BUNDLE_PLAN = Plan
            .builder()
            .bundle(BundlePlan
                    .builder()
                    .name(new BundleName("foo"))
                    .groupId(new GroupId("org.foo"))
                    .artifactId(new ArtifactId("foo"))
                    .version(new Version("1"))
                    .instance("bar", ImmutableMap.of(VERSION, "2"))
                    .instance("baz", ImmutableMap.of(VERSION, "3"))
                    .build())
            .build();
    private static final String BUNDLE_PLAN_YAML = ""
            + "bundles:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      bar:\n"
            + "        version: 2\n"
            + "      baz:\n"
            + "        version: 3\n";

    @Test
    public void shouldDeserializePlanWithBundleDeploymentWithVars() throws Exception {
        Plan plan = Plan.load(variables, new StringReader(BUNDLE_PLAN_YAML), "yaml-bundle");

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
    private static final LoggerPlan LOGGER = LoggerPlan
            .builder()
            .category(LoggerCategory.of("some.logger.category"))
            .level(TRACE)
            .handler("CONSOLE")
            .useParentHandlers(true)
            .build();
    private static final Plan ONE_LOGGER_PLAN = Plan
            .builder()
            .logger(LOGGER)
            .build();

    @Test
    public void shouldDeserializePlanWithOneLogger() throws Exception {
        Plan plan = Plan.load(variables, new StringReader(ONE_LOGGER_YAML), "1log");

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
            + "    type: periodic-rotating-file\n"
            + "    level: INFO\n"
            + "    format: the-format\n"
            + "    file: the-file\n"
            + "    suffix: the-suffix\n";
    private static final LogHandlerPlan LOGHANDLER = LogHandlerPlan
            .builder()
            .name(new LogHandlerName("FOO"))
            .level(INFO)
            .type(periodicRotatingFile)
            .file("the-file")
            .suffix("the-suffix")
            .format("the-format")
            .build();
    private static final Plan ONE_LOGHANDLER_PLAN = Plan
            .builder()
            .logHandler(LOGHANDLER)
            .build();

    @Test
    public void shouldDeserializePlanWithOneLogHandler() throws Exception {
        Plan plan = Plan.load(variables, new StringReader(ONE_LOGHANDLER_YAML), "1log-h");

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
            + "    type: custom\n"
            + "    level: INFO\n"
            + "    format: the-format\n"
            + "    module: org.foo\n"
            + "    class: org.foo.MyHandler\n";
    private static final Plan CUSTOM_HANDLER_PLAN = Plan
            .builder()
            .logHandler(LogHandlerPlan
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
        Plan plan = Plan.load(variables, new StringReader(CUSTOM_HANDLER_YAML), "custom-h");

        assertThat(plan).isEqualTo(CUSTOM_HANDLER_PLAN);
    }

    @Test
    public void shouldSerializePlanWithCustomLogHandler() throws Exception {
        String yaml = CUSTOM_HANDLER_PLAN.toYaml();

        assertThat(yaml).isEqualTo(CUSTOM_HANDLER_YAML);
    }

    @Test
    public void shouldFailToDeserializePlanWithCustomLogHandlerWithoutModule() throws Exception {
        Throwable thrown = catchThrowable(() -> Plan.load(variables, new StringReader(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: INFO\n"
                + "    type: custom\n"
                + "    format: the-format\n"
                + "    class: org.foo.MyHandler\n"
        ), "xm"));

        assertThat(thrown).hasStackTraceContaining("log-handler [FOO] is of type [custom], so it requires a 'module'");
    }

    @Test
    public void shouldFailToDeserializePlanWithCustomLogHandlerWithoutClass() throws Exception {
        Throwable thrown = catchThrowable(() -> Plan.load(variables, new StringReader(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: INFO\n"
                + "    type: custom\n"
                + "    format: the-format\n"
                + "    module: org.foo\n"
        ), "xc"));

        assertThat(thrown).hasStackTraceContaining("log-handler [FOO] is of type [custom], so it requires a 'class'");
    }
}
