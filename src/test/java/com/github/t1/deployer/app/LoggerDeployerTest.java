package com.github.t1.deployer.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.ConstraintViolation;

import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class LoggerDeployerTest extends AbstractDeployerTest {
    @Test
    public void shouldAddLogger() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldNotAddExistingLogger() {
        givenLogger("com.github.t1.deployer.app").level(DEBUG).deployed();

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n");

        // #after(): no add nor update
        assertThat(audits.asList()).isEmpty();
    }


    @Test
    public void shouldUpdateLogLevel() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG).deployed();

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: INFO\n");

        fixture.level(INFO).verifyChanged(audits);
    }


    @Test
    public void shouldAddLoggerWithExplicitState() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: deployed\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldFailToAddLoggerWithoutLevel() {
        givenLogger("com.github.t1.deployer.app");

        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    state: deployed\n"));

        assertThat(unpackViolations(thrown))
                .extracting(ConstraintViolation::getMessage, ArtifactDeployerTest::pathString)
                .containsExactly(tuple("may not be null", "level"));
    }


    // TODO shouldAddWithLoggerHandlers
    // TODO shouldUpdateLoggerHandlers
    // TODO shouldAddUseParentHandlers
    // TODO shouldUpdateUseParentHandlers


    @Test
    public void shouldRemoveExistingLoggerWhenStateIsUndeployed() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG).deployed();

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: undeployed\n");

        fixture.verifyRemoved(audits);
    }


    @Test
    public void shouldRemoveNonExistingLoggerWhenStateIsUndeployed() {
        givenLogger("com.github.t1.deployer.app").level(DEBUG);

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: undeployed\n");

        // #after(): not undeployed
        assertThat(audits.asList()).isEmpty();
    }


    // TODO shouldRemoveLoggerWhenManaged


    @Test
    public void shouldAddPeriodicRotatingFileHandlerAsDefault() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandler() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandlerWithDefaultFile() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("FOO")
                .suffix("the-suffix")
                .formatter("the-formatter");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandlerWithDefaultSuffix() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix(".yyyy-MM-dd")
                .formatter("the-formatter");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    formatter: the-formatter\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldNotAddExistingHandler() {
        givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        // #after(): not added/updated
        assertThat(audits.asList()).isEmpty();
    }


    @Test
    public void shouldUpdateHandlerLevel() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(DEBUG)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.level(ALL).verifyUpdatedLogLevel(audits);
    }


    @Test
    public void shouldUpdateHandlerFile() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-old-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-new-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.file("the-new-file").verifyUpdatedFile(audits);
    }


    @Test
    public void shouldUpdateHandlerSuffix() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-old-suffix")
                .formatter("the-formatter")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-new-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.suffix("the-new-suffix").verifyUpdatedSuffix(audits);
    }


    @Test
    public void shouldUpdateHandlerFormatter() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-old-formatter")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-new-formatter\n");

        fixture.formatter("the-new-formatter").verifyUpdatedFormatter(audits);
    }


    @Test
    public void shouldUpdateHandlerFileAndFormatter() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-old-file")
                .suffix("the-suffix")
                .formatter("the-old-formatter")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-new-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-new-formatter\n");

        fixture.file("the-new-file").formatter("the-new-formatter");
        fixture.verifyLogHandler().correctFile(fixture.getFile());
        fixture.verifyLogHandler().correctFormatter(fixture.getFormatter());
        assertThat(audits.asList()).isEmpty();
    }


    // TODO shouldRemoveHandlerWhenStateIsUndeployed
    // TODO shouldRemoveHandlerWhenManaged
    // TODO shouldAddConsoleHandler
    // TODO shouldAddCustomHandler

    // TODO shouldAddLoggerAndHandler
}
