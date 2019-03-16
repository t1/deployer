package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Age;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.BundleName;
import com.github.t1.deployer.model.BundlePlan;
import com.github.t1.deployer.model.DataSourceName;
import com.github.t1.deployer.model.DataSourcePlan;
import com.github.t1.deployer.model.DataSourcePlan.PoolPlan;
import com.github.t1.deployer.model.DeployablePlan;
import com.github.t1.deployer.model.DeploymentName;
import com.github.t1.deployer.model.Expressions;
import com.github.t1.deployer.model.Expressions.Match;
import com.github.t1.deployer.model.Expressions.Resolver;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.LogHandlerName;
import com.github.t1.deployer.model.LogHandlerPlan;
import com.github.t1.deployer.model.LoggerCategory;
import com.github.t1.deployer.model.LoggerPlan;
import com.github.t1.deployer.model.Plan;
import com.github.t1.deployer.model.Version;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.io.StringReader;
import java.net.URI;
import java.time.Duration;

import static com.github.t1.deployer.model.ArtifactType.war;
import static com.github.t1.deployer.model.LogHandlerType.custom;
import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.deployer.testtools.TestData.VERSION;
import static com.github.t1.log.LogLevel.INFO;
import static com.github.t1.log.LogLevel.TRACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlanSerializationTest {
    private static final DeployablePlan FOO = DeployablePlan
            .builder()
            .type(war)
            .name(new DeploymentName("foo"))
            .groupId(new GroupId("org.foo"))
            .artifactId(new ArtifactId("foo-war"))
            .version(new Version("1"))
            .build();
    private static Expressions expressions = mock(Expressions.class);

    static {
        when(expressions.resolve(anyString())).then(invocation -> invocation.getArgument(0));
        when(expressions.resolve(anyString(), any())).then(invocation -> invocation.getArgument(0));
        when(expressions.resolver()).then(i -> (Resolver) expression -> Match.PROCEED);
    }


    @Test
    public void shouldSerializeEmptyPlan() {
        Plan plan = Plan.builder().build();

        String yaml = plan.toYaml();

        assertThat(yaml).isEqualTo("{}\n");
    }

    @Test
    public void shouldDeserializeEmptyPlan() {
        Plan plan = Plan.load(expressions, new StringReader("{}"), "empty");

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
    public void shouldDeserializePlanWithOneDeployment() {
        Plan plan = Plan.load(expressions, new StringReader(ONE_DEPLOYMENT_YAML), "yaml1");

        assertThat(plan).isEqualTo(ONE_DEPLOYMENT_PLAN);
    }

    @Test
    public void shouldSerializePlanWithOneDeployment() {
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
    public void shouldDeserializePlanWithTwoDeployments() {
        Plan plan = Plan.load(expressions, new StringReader(TWO_DEPLOYMENTS_YAML), "yaml2");

        assertThat(plan).isEqualTo(TWO_DEPLOYMENTS_PLAN);
    }

    @Test
    public void shouldSerializePlanWithTwoDeployments() {
        String yaml = TWO_DEPLOYMENTS_PLAN.toYaml();

        assertThat(yaml).isEqualTo(TWO_DEPLOYMENTS_YAML);
    }


    private static final String BUNDLE_YAML = ""
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

    @Test
    public void shouldDeserializePlanWithBundleDeploymentWithVars() {
        Plan plan = Plan.load(expressions, new StringReader(BUNDLE_YAML), "yaml-bundle");

        assertThat(plan).isEqualTo(BUNDLE_PLAN);
    }

    @Test
    public void shouldSerializePlanWithBundleDeploymentWithVars() {
        String yaml = BUNDLE_PLAN.toYaml();

        assertThat(yaml).isEqualTo(BUNDLE_YAML);
    }


    private static final String ONE_LOGGER_YAML = ""
            + "loggers:\n"
            + "  some.logger.category:\n"
            + "    level: TRACE\n"
            + "    handlers:\n"
            + "    - CONSOLE\n"
            + "    use-parent-handlers: true\n";
    private static final Plan ONE_LOGGER_PLAN = Plan
            .builder()
            .logger(LoggerPlan
                    .builder()
                    .category(LoggerCategory.of("some.logger.category"))
                    .level(TRACE)
                    .handler("CONSOLE")
                    .useParentHandlers(true)
                    .build())
            .build();

    @Test
    public void shouldDeserializePlanWithOneLogger() {
        Plan plan = Plan.load(expressions, new StringReader(ONE_LOGGER_YAML), "1log");

        assertThat(plan).isEqualTo(ONE_LOGGER_PLAN);
    }

    @Test
    public void shouldSerializePlanWithOneLogger() {
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
    private static final Plan ONE_LOGHANDLER_PLAN = Plan
            .builder()
            .logHandler(LogHandlerPlan
                    .builder()
                    .name(new LogHandlerName("FOO"))
                    .level(INFO)
                    .type(periodicRotatingFile)
                    .file("the-file")
                    .suffix("the-suffix")
                    .format("the-format")
                    .build())
            .build();

    @Test
    public void shouldDeserializePlanWithOneLogHandler() {
        Plan plan = Plan.load(expressions, new StringReader(ONE_LOGHANDLER_YAML), "1log-h");

        assertThat(plan).isEqualTo(ONE_LOGHANDLER_PLAN);
    }

    @Test
    public void shouldSerializePlanWithOneLogHandler() {
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
    public void shouldDeserializePlanWithCustomLogHandler() {
        Plan plan = Plan.load(expressions, new StringReader(CUSTOM_HANDLER_YAML), "custom-h");

        assertThat(plan).isEqualTo(CUSTOM_HANDLER_PLAN);
    }

    @Test
    public void shouldSerializePlanWithCustomLogHandler() {
        String yaml = CUSTOM_HANDLER_PLAN.toYaml();

        assertThat(yaml).isEqualTo(CUSTOM_HANDLER_YAML);
    }

    @Test
    public void shouldFailToDeserializePlanWithCustomLogHandlerWithoutModule() {
        Throwable thrown = catchThrowable(() -> Plan.load(expressions, new StringReader(""
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
    public void shouldFailToDeserializePlanWithCustomLogHandlerWithoutClass() {
        Throwable thrown = catchThrowable(() -> Plan.load(expressions, new StringReader(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: INFO\n"
                + "    type: custom\n"
                + "    format: the-format\n"
                + "    module: org.foo\n"
        ), "xc"));

        assertThat(thrown).hasStackTraceContaining("log-handler [FOO] is of type [custom], so it requires a 'class'");
    }


    private static final String DATASOURCE_YAML = ""
            + "data-sources:\n"
            + "  FOO:\n"
            + "    uri: jdbc:h2:mem:test\n"
            + "    jndi-name: java:/datasources/TestDS\n"
            + "    driver: h3\n"
            + "    user-name: joe\n"
            + "    password: secret\n"
            + "    pool:\n"
            + "      min: 3\n"
            + "      initial: 5\n"
            + "      max: 10\n"
            + "      max-age: 3600 ms\n";
    private static final Plan DATASOURCE_PLAN = Plan
            .builder()
            .dataSource(DataSourcePlan
                    .builder()
                    .name(new DataSourceName("FOO"))
                    .uri(URI.create("jdbc:h2:mem:test"))
                    .jndiName("java:/datasources/TestDS")
                    .driver("h3")
                    .userName("joe")
                    .password("secret")
                    .pool(PoolPlan
                            .builder()
                            .min(3)
                            .initial(5)
                            .max(10)
                            .maxAge(new Age(Duration.ofMillis(3600)))
                            .build())
                    .build())
            .build();

    @Test
    public void shouldDeserializePlanWithDataSource() {
        Plan plan = Plan.load(expressions, new StringReader(DATASOURCE_YAML), "ds-h");

        assertThat(plan).isEqualTo(DATASOURCE_PLAN);
    }

    @Test
    public void shouldSerializePlanWithDataSource() {
        String yaml = DATASOURCE_PLAN.toYaml();

        assertThat(yaml).isEqualTo(DATASOURCE_YAML);
    }
}
