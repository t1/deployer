package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Plan.*;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.LogHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

public class ReadEffectivePlanTest extends AbstractDeployerTest {
    private static final LoggerPlan ROOT = LoggerPlan
            .builder()
            .category(LoggerCategory.ROOT)
            .state(deployed)
            .level(INFO)
            .handler("CONSOLE")
            .handler("FILE")
            .build();

    private static List<DeployablePlan> deployables(Plan plan) {
        return plan.deployables().collect(Collectors.toList());
    }

    private static List<LoggerPlan> loggers(Plan plan) {
        return plan.loggers().collect(Collectors.toList());
    }

    private static List<LogHandlerPlan> logHandlers(Plan plan) {
        return plan.logHandlers().collect(Collectors.toList());
    }

    private static List<DataSourcePlan> dataSources(Plan plan) {
        return plan.dataSources().collect(Collectors.toList());
    }

    @Test
    public void shouldReadZeroDeployments() throws Exception {
        Plan plan = boundary.getEffectivePlan();

        assertThat(deployables(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneDeployment() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(deployables(plan)).containsExactly(foo.asPlan());
    }

    @Test
    public void shouldReadTwoDeployments() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("2.0").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(deployables(plan)).containsExactly(bar.asPlan(), foo.asPlan());
    }


    @Test
    public void shouldReadZeroLoggers() throws Exception {
        Plan plan = boundary.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT);
    }

    @Test
    public void shouldReadOneLogger() throws Exception {
        LoggerFixture foo = givenLogger("foo").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT, foo.asPlan());
    }

    @Test
    public void shouldReadTwoLoggers() throws Exception {
        LoggerFixture foo = givenLogger("foo").deployed();
        LoggerFixture bar = givenLogger("bar").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT, bar.asPlan(), foo.asPlan()); // sorted!
    }


    @Test
    public void shouldReadZeroLogHandlers() throws Exception {
        Plan plan = boundary.getEffectivePlan();

        assertThat(logHandlers(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneLogHandler() throws Exception {
        LogHandlerFixture foo = givenLogHandler(periodicRotatingFile, "foo").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(logHandlers(plan)).containsExactly(foo.asPlan());
    }

    @Test
    public void shouldReadTwoLogHandlers() throws Exception {
        LogHandlerFixture foo = givenLogHandler(periodicRotatingFile, "foo").deployed();
        LogHandlerFixture bar = givenLogHandler(custom, "bar")
                .module("org.bar")
                .class_("org.bar.MyHandler")
                .property("bar", "baz")
                .deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(logHandlers(plan)).containsExactly(bar.asPlan(), foo.asPlan());
    }


    @Test
    public void shouldReadZeroDataSources() throws Exception {
        Plan plan = boundary.getEffectivePlan();

        assertThat(dataSources(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneDataSource() throws Exception {
        DataSourceFixture foo = givenDataSource("foo").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(dataSources(plan)).containsExactly(foo.asPlan());
    }

    @Test
    public void shouldReadTwoDataSources() throws Exception {
        DataSourceFixture foo = givenDataSource("foo").deployed();
        DataSourceFixture bar = givenDataSource("bar").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(dataSources(plan)).containsExactly(bar.asPlan(), foo.asPlan()); // sorted!
    }
}
