package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.LoggerCategory;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.t1.deployer.container.LogHandlerType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

public class ReadEffectivePlanTest extends AbstractDeployerTest {
    private static final LoggerConfig ROOT = LoggerConfig
            .builder()
            .category(LoggerCategory.ROOT)
            .state(deployed)
            .level(INFO)
            .handler("CONSOLE")
            .handler("FILE")
            .build();

    private static List<DeployableConfig> deployables(ConfigurationPlan plan) {
        return plan.deployables().collect(Collectors.toList());
    }

    private static List<LoggerConfig> loggers(ConfigurationPlan plan) {
        return plan.loggers().collect(Collectors.toList());
    }

    private static List<LogHandlerConfig> logHandlers(ConfigurationPlan plan) {
        return plan.logHandlers().collect(Collectors.toList());
    }

    @Test
    public void shouldReadZeroDeployments() throws Exception {
        ConfigurationPlan plan = deployer.getEffectivePlan();

        assertThat(deployables(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneDeployment() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();

        ConfigurationPlan plan = deployer.getEffectivePlan();

        assertThat(deployables(plan)).containsExactly(foo.asConfig());
    }

    @Test
    public void shouldReadTwoDeployments() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("2.0").deployed();

        ConfigurationPlan plan = deployer.getEffectivePlan();

        assertThat(deployables(plan)).containsExactly(bar.asConfig(), foo.asConfig());
    }


    @Test
    public void shouldReadZeroLoggers() throws Exception {
        ConfigurationPlan plan = deployer.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT);
    }

    @Test
    public void shouldReadOneLogger() throws Exception {
        LoggerFixture foo = givenLogger("foo").deployed();

        ConfigurationPlan plan = deployer.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT, foo.asConfig());
    }

    @Test
    public void shouldReadTwoLoggers() throws Exception {
        LoggerFixture foo = givenLogger("foo").deployed();
        LoggerFixture bar = givenLogger("bar").deployed();

        ConfigurationPlan plan = deployer.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT, bar.asConfig(), foo.asConfig()); // sorted!
    }


    @Test
    public void shouldReadZeroLogHandlers() throws Exception {
        ConfigurationPlan plan = deployer.getEffectivePlan();

        assertThat(logHandlers(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneLogHandler() throws Exception {
        LogHandlerFixture foo = givenLogHandler(periodicRotatingFile, "foo").deployed();

        ConfigurationPlan plan = deployer.getEffectivePlan();

        assertThat(logHandlers(plan)).containsExactly(foo.asConfig());
    }

    @Test
    public void shouldReadTwoLogHandlers() throws Exception {
        LogHandlerFixture foo = givenLogHandler(periodicRotatingFile, "foo").deployed();
        LogHandlerFixture bar = givenLogHandler(custom, "bar")
                .module("org.bar")
                .class_("org.bar.MyHandler")
                .property("bar", "baz")
                .deployed();

        ConfigurationPlan plan = deployer.getEffectivePlan();

        assertThat(logHandlers(plan)).containsExactly(bar.asConfig(), foo.asConfig());
    }
}
