package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import org.junit.Test;

import java.io.StringReader;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

public class ConfigurationPlanSerializationTest {
    private static final DeploymentConfig FOO = DeploymentConfig
            .builder()
            .name(new DeploymentName("foo"))
            .groupId(new GroupId("org.foo"))
            .artifactId(new ArtifactId("foo-war"))
            .version(new Version("1"))
            .type(war)
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
            .artifact(new DeploymentName("foo"), FOO)
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
            .handler(new LogHandlerName("CONSOLE"))
            .useParentHandlers(true)
            .build();
    private static final ConfigurationPlan ONE_LOGGER_PLAN = ConfigurationPlan
            .builder()
            .logger(LOGGER.getCategory(), LOGGER)
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
            + "    file: the-file\n"
            + "    suffix: the-suffix\n"
            + "    format: the-format\n";
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
            .logHandler(LOGHANDLER.getName(), LOGHANDLER)
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
