package com.github.t1.deployer.app;

import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.DataSourcePlan.PoolPlan;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.*;

import java.io.StringReader;
import java.net.URI;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.LogHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PlanSerializationTest {
    private static final Expressions EXPRESSIONS = mock(Expressions.class);

    static {
        when(EXPRESSIONS.resolve(anyString(), any())).then(invocation -> invocation.getArgument(0));
        when(EXPRESSIONS.resolver(any(CharSequence.class))).then(i -> new Expressions.Resolver() {});
    }

    @Nested
    class WhenEmpty {
        @Test
        public void shouldSerialize() throws Exception {
            Plan plan = Plan.builder().build();

            String yaml = plan.toYaml();

            assertThat(yaml).isEqualTo("{}\n");
        }

        @Test
        public void shouldDeserialize() throws Exception {
            Plan plan = Plan.load(EXPRESSIONS, new StringReader("{}"), "empty");

            assertThat(plan).isEqualTo(Plan.builder().build());
        }
    }


    @Nested
    class WithDeployment {
        private static final String ONE_DEPLOYMENT_YAML = ""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo-war\n"
                + "    version: 1\n"
                + "    type: war\n";

        private final Plan ONE_DEPLOYMENT_PLAN = Plan
                .builder()
                .deployable(DeployablePlan
                        .builder()
                        .type(war)
                        .name(new DeploymentName("foo"))
                        .groupId(new GroupId("org.foo"))
                        .artifactId(new ArtifactId("foo-war"))
                        .version(new Version("1"))
                        .build())
                .build();

        @Test
        public void shouldSerialize() throws Exception {
            String yaml = ONE_DEPLOYMENT_PLAN.toYaml();

            assertThat(yaml).isEqualTo(ONE_DEPLOYMENT_YAML);
        }

        @Test
        public void shouldDeserialize() throws Exception {
            Plan plan = Plan.load(EXPRESSIONS, new StringReader(ONE_DEPLOYMENT_YAML), "yaml1");

            assertThat(plan).isEqualTo(ONE_DEPLOYMENT_PLAN);
        }
    }

    @Nested
    class WithTwoDeployments {
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

        private final Plan TWO_DEPLOYMENTS_PLAN = Plan
                .builder()
                .deployable(DeployablePlan
                        .builder()
                        .type(war)
                        .name(new DeploymentName("foo"))
                        .groupId(new GroupId("org.foo"))
                        .artifactId(new ArtifactId("foo-war"))
                        .version(new Version("1"))
                        .build())
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
        public void shouldSerialize() throws Exception {
            String yaml = TWO_DEPLOYMENTS_PLAN.toYaml();

            assertThat(yaml).isEqualTo(TWO_DEPLOYMENTS_YAML);
        }

        @Test
        public void shouldDeserialize() throws Exception {
            Plan plan = Plan.load(EXPRESSIONS, new StringReader(TWO_DEPLOYMENTS_YAML), "yaml2");

            assertThat(plan).isEqualTo(TWO_DEPLOYMENTS_PLAN);
        }
    }


    @Nested
    class WithBundle {
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
        private final Plan BUNDLE_PLAN = Plan
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
        public void shouldSerialize() throws Exception {
            String yaml = BUNDLE_PLAN.toYaml();

            assertThat(yaml).isEqualTo(BUNDLE_YAML);
        }

        @Test
        public void shouldDeserialize() throws Exception {
            Plan plan = Plan.load(EXPRESSIONS, new StringReader(BUNDLE_YAML), "yaml-bundle");

            assertThat(plan).isEqualTo(BUNDLE_PLAN);
        }
    }

    @Nested
    class WithLogger {
        private static final String ONE_LOGGER_YAML = ""
                + "loggers:\n"
                + "  some.logger.category:\n"
                + "    level: TRACE\n"
                + "    handlers:\n"
                + "    - CONSOLE\n"
                + "    use-parent-handlers: true\n";
        private final Plan ONE_LOGGER_PLAN = Plan
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
        public void shouldSerialize() throws Exception {
            String yaml = ONE_LOGGER_PLAN.toYaml();

            assertThat(yaml).isEqualTo(ONE_LOGGER_YAML);
        }

        @Test
        public void shouldDeserialize() throws Exception {
            Plan plan = Plan.load(EXPRESSIONS, new StringReader(ONE_LOGGER_YAML), "1log");

            assertThat(plan).isEqualTo(ONE_LOGGER_PLAN);
        }
    }


    @Nested
    class WithLogHandler {
        private static final String ONE_LOGHANDLER_YAML = ""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: INFO\n"
                + "    format: the-format\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n";
        private final Plan ONE_LOGHANDLER_PLAN = Plan
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
        public void shouldSerialize() throws Exception {
            String yaml = ONE_LOGHANDLER_PLAN.toYaml();

            assertThat(yaml).isEqualTo(ONE_LOGHANDLER_YAML);
        }

        @Test
        public void shouldDeserialize() throws Exception {
            Plan plan = Plan.load(EXPRESSIONS, new StringReader(ONE_LOGHANDLER_YAML), "1log-h");

            assertThat(plan).isEqualTo(ONE_LOGHANDLER_PLAN);
        }
    }


    @Nested
    class WithCustomLogHandler {
        private static final String CUSTOM_HANDLER_YAML = ""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    level: INFO\n"
                + "    format: the-format\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n";
        private final Plan CUSTOM_HANDLER_PLAN = Plan
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
        public void shouldSerialize() throws Exception {
            String yaml = CUSTOM_HANDLER_PLAN.toYaml();

            assertThat(yaml).isEqualTo(CUSTOM_HANDLER_YAML);
        }

        @Test
        public void shouldDeserialize() throws Exception {
            Plan plan = Plan.load(EXPRESSIONS, new StringReader(CUSTOM_HANDLER_YAML), "custom-h");

            assertThat(plan).isEqualTo(CUSTOM_HANDLER_PLAN);
        }

        @Test
        public void shouldFailToDeserializeWithoutModule() throws Exception {
            Throwable thrown = catchThrowable(() -> Plan.load(EXPRESSIONS, new StringReader(""
                    + "log-handlers:\n"
                    + "  FOO:\n"
                    + "    level: INFO\n"
                    + "    type: custom\n"
                    + "    format: the-format\n"
                    + "    class: org.foo.MyHandler\n"
            ), "xm"));

            assertThat(thrown).hasStackTraceContaining(
                    "log-handler [FOO] is of type [custom], so it requires a 'module'");
        }

        @Test
        public void shouldFailToDeserializeWithoutClass() throws Exception {
            Throwable thrown = catchThrowable(() -> Plan.load(EXPRESSIONS, new StringReader(""
                    + "log-handlers:\n"
                    + "  FOO:\n"
                    + "    level: INFO\n"
                    + "    type: custom\n"
                    + "    format: the-format\n"
                    + "    module: org.foo\n"
            ), "xc"));

            assertThat(thrown).hasStackTraceContaining(
                    "log-handler [FOO] is of type [custom], so it requires a 'class'");
        }
    }

    @Nested
    class WithDataSource {
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
        private final Plan DATASOURCE_PLAN = Plan
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
                                .maxAge(Age.ofMillis(3600))
                                .build())
                        .build())
                .build();

        @Test
        public void shouldSerialize() throws Exception {
            String yaml = DATASOURCE_PLAN.toYaml();

            assertThat(yaml).isEqualTo(DATASOURCE_YAML);
        }

        @Test
        public void shouldDeserialize() throws Exception {
            Plan plan = Plan.load(EXPRESSIONS, new StringReader(DATASOURCE_YAML), "ds-h");

            assertThat(plan).isEqualTo(DATASOURCE_PLAN);
        }
    }
}
