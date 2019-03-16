package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTests.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.model.DataSourcePlan;
import com.github.t1.deployer.model.DeployablePlan;
import com.github.t1.deployer.model.LogHandlerPlan;
import com.github.t1.deployer.model.LoggerCategory;
import com.github.t1.deployer.model.LoggerPlan;
import com.github.t1.deployer.model.Plan;
import com.github.t1.log.LogLevel;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.t1.deployer.model.DeploymentState.deployed;
import static com.github.t1.deployer.model.LogHandlerType.custom;
import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.log.LogLevel.ALL;
import static com.github.t1.log.LogLevel.DEBUG;
import static com.github.t1.log.LogLevel.ERROR;
import static com.github.t1.log.LogLevel.INFO;
import static com.github.t1.log.LogLevel.OFF;
import static com.github.t1.log.LogLevel.TRACE;
import static com.github.t1.log.LogLevel.WARN;
import static org.assertj.core.api.Assertions.assertThat;

public class ReadEffectivePlanTest extends AbstractDeployerTests {
    private static final LoggerPlan ROOT = new LoggerPlan(LoggerCategory.ROOT)
        .setState(deployed)
        .setLevel(INFO)
        .addHandler("CONSOLE")
        .addHandler("FILE");

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
    public void shouldReadZeroDeployments() {
        Plan plan = boundary.getEffectivePlan();

        assertThat(deployables(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneDeployment() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(deployables(plan)).containsExactly(foo.asPlan());
    }

    @Test
    public void shouldReadTwoDeployments() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("2.0").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(deployables(plan)).containsExactly(bar.asPlan(), foo.asPlan());
    }


    @Test
    public void shouldReadZeroLoggers() {
        Plan plan = boundary.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT);
    }

    @Test
    public void shouldReadOneLogger() {
        LoggerFixture foo = givenLogger("foo").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT, foo.asPlan());
    }

    @Test
    public void shouldReadTwoLoggers() {
        LoggerFixture foo = givenLogger("foo").deployed();
        LoggerFixture bar = givenLogger("bar").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT, bar.asPlan(), foo.asPlan()); // sorted!
    }

    @Test public void shouldReadOneLoggerAtLevelOff() { shouldReadOneLoggerAtLevel("OFF", OFF); }

    @Test public void shouldReadOneLoggerAtLevelTrace() { shouldReadOneLoggerAtLevel("TRACE", TRACE); }

    @Test public void shouldReadOneLoggerAtLevelFinest() { shouldReadOneLoggerAtLevel("FINEST", TRACE); }

    @Test public void shouldReadOneLoggerAtLevelFiner() { shouldReadOneLoggerAtLevel("FINER", DEBUG); }

    @Test public void shouldReadOneLoggerAtLevelFine() { shouldReadOneLoggerAtLevel("FINE", DEBUG); }

    @Test public void shouldReadOneLoggerAtLevelDebug() { shouldReadOneLoggerAtLevel("DEBUG", DEBUG); }

    @Test public void shouldReadOneLoggerAtLevelConfig() { shouldReadOneLoggerAtLevel("CONFIG", INFO); }

    @Test public void shouldReadOneLoggerAtLevelInfo() { shouldReadOneLoggerAtLevel("INFO", INFO); }

    @Test public void shouldReadOneLoggerAtLevelWarn() { shouldReadOneLoggerAtLevel("WARN", WARN); }

    @Test public void shouldReadOneLoggerAtLevelWarning() { shouldReadOneLoggerAtLevel("WARNING", WARN); }

    @Test public void shouldReadOneLoggerAtLevelSevere() { shouldReadOneLoggerAtLevel("SEVERE", ERROR); }

    @Test public void shouldReadOneLoggerAtLevelFatal() { shouldReadOneLoggerAtLevel("FATAL", ERROR); }

    @Test public void shouldReadOneLoggerAtLevelError() { shouldReadOneLoggerAtLevel("ERROR", ERROR); }

    @Test public void shouldReadOneLoggerAtLevelAll() { shouldReadOneLoggerAtLevel("ALL", ALL); }

    private void shouldReadOneLoggerAtLevel(String actual, LogLevel expected) {
        LoggerFixture foo = givenLogger("foo").level(actual).deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(loggers(plan)).containsExactly(ROOT, foo.level(expected).asPlan());
    }


    @Test
    public void shouldReadZeroLogHandlers() {
        Plan plan = boundary.getEffectivePlan();

        assertThat(logHandlers(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneLogHandler() {
        LogHandlerFixture foo = givenLogHandler(periodicRotatingFile, "foo").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(logHandlers(plan)).containsExactly(foo.asPlan());
    }

    @Test
    public void shouldReadTwoLogHandlers() {
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
    public void shouldReadZeroDataSources() {
        Plan plan = boundary.getEffectivePlan();

        assertThat(dataSources(plan)).isEmpty();
    }

    @Test
    public void shouldReadOneDataSource() {
        DataSourceFixture foo = givenDataSource("foo").credentials("joe", "secret").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(dataSources(plan)).containsExactly(foo.password(null).asPlan());
    }

    @Test
    public void shouldReadTwoDataSources() {
        DataSourceFixture foo = givenDataSource("foo").credentials("joe", "secret").deployed();
        DataSourceFixture bar = givenDataSource("bar").credentials("joe", "secret").deployed();

        Plan plan = boundary.getEffectivePlan();

        assertThat(dataSources(plan))
            .containsExactly(bar.password(null).asPlan(), foo.password(null).asPlan()); // sorted!
    }
}
