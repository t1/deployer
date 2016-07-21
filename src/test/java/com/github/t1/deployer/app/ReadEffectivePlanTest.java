package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.LoggerCategory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class ReadEffectivePlanTest extends AbstractDeployerTest {
    private static final LoggerConfig ROOT = LoggerConfig
            .builder()
            .category(LoggerCategory.ROOT)
            .state(deployed)
            .level(INFO)
            .handler("CONSOLE")
            .handler("FILE")
            .useParentHandlers(true)
            .build();

    private static List<DeploymentConfig> artifacts(ConfigurationPlan plan) {
        return plan.artifacts().collect(Collectors.toList());
    }

    private static List<LoggerConfig> loggers(ConfigurationPlan plan) {
        return plan.loggers().collect(Collectors.toList());
    }

    private static List<LogHandlerConfig> logHandlers(ConfigurationPlan plan) {
        return plan.logHandlers().collect(Collectors.toList());
    }

    @Test
    public void shouldReadZeroDeployments() throws Exception {
        ConfigurationPlan plan = deployer.effectivePlan();

        assertThat(artifacts(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneDeployment() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();

        ConfigurationPlan plan = deployer.effectivePlan();

        assertThat(artifacts(plan)).containsExactly(foo.asConfig());
    }

    @Test
    public void shouldReadTwoDeployments() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("2.0").deployed();

        ConfigurationPlan plan = deployer.effectivePlan();

        assertThat(artifacts(plan)).containsExactly(foo.asConfig(), bar.asConfig());
    }


    @Test
    public void shouldReadZeroLoggers() throws Exception {
        ConfigurationPlan plan = deployer.effectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT);
    }

    @Test
    public void shouldReadOneLogger() throws Exception {
        LoggerFixture foo = givenLogger("foo").deployed();

        ConfigurationPlan plan = deployer.effectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT, foo.asConfig());
    }

    @Test
    public void shouldReadTwoLoggers() throws Exception {
        LoggerFixture foo = givenLogger("foo").deployed();
        LoggerFixture bar = givenLogger("bar").deployed();

        ConfigurationPlan plan = deployer.effectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT, bar.asConfig(), foo.asConfig()); // sorted!
    }


    @Test
    public void shouldReadZeroLogHandlers() throws Exception {
        ConfigurationPlan plan = deployer.effectivePlan();

        assertThat(logHandlers(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneLogHandler() throws Exception {
        LogHandlerFixture foo = givenLogHandler(periodicRotatingFile, "foo").deployed();

        ConfigurationPlan plan = deployer.effectivePlan();

        List<LogHandlerConfig> actual = logHandlers(plan);
        LogHandlerConfig expected = foo.asConfig();
        System.out.println(actual.equals(expected));
        assertThat(actual).containsExactly(expected);
    }

    @Test
    public void shouldReadTwoLogHandlers() throws Exception {
        LogHandlerFixture foo = givenLogHandler(periodicRotatingFile, "foo").deployed();
        LogHandlerFixture bar = givenLogHandler(periodicRotatingFile, "bar").deployed();

        ConfigurationPlan plan = deployer.effectivePlan();

        assertThat(logHandlers(plan)).containsExactly(foo.asConfig(), bar.asConfig());
    }
}
