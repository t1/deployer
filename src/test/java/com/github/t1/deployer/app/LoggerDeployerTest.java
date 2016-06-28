package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LoggerAudit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.ConstraintViolation;
import java.util.List;

import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class LoggerDeployerTest extends AbstractDeployerTest {
    @Test
    public void shouldAddLogger() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        List<Audit> audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
        ).asList();

        fixture.verifyAdded();
        assertThat(audits).containsExactly(LoggerAudit.of(fixture.getCategory()).level(DEBUG).deployed());
    }


    @Test
    public void shouldNotAddExistingLogger() {
        givenLogger("com.github.t1.deployer.app").level(DEBUG).deployed();

        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n");

        // #after(): no add nor update
    }


    @Test
    public void shouldUpdateLogLevel() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG).deployed();

        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: INFO\n");

        fixture.verifyLogger().correctLevel(INFO);
    }


    @Test
    public void shouldAddLoggerWithExplicitState() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: deployed\n");

        fixture.verifyAdded();
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

        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: undeployed\n");

        fixture.verifyLogger().remove();
    }


    @Test
    public void shouldRemoveNonExistingLoggerWhenStateIsUndeployed() {
        givenLogger("com.github.t1.deployer.app").level(DEBUG);

        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: undeployed\n");

        // #after(): not undeployed
    }


    // TODO shouldRemoveLoggerWhenManaged


    @Test
    public void shouldAddPeriodicRotatingFileHandlerAsDefault() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter");

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyAdded();
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandler() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter");

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyAdded();
    }


    @Test
    public void shouldNotAddExistingHandler() {
        givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .deployed();

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        // #after(): not added/updated
    }


    @Test
    public void shouldUpdateHandlerLevel() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(DEBUG)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .deployed();

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyUpdated().correctLevel(ALL);
    }


    @Test
    public void shouldUpdateHandlerFile() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-old-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .deployed();

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-new-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyUpdated().correctFile("the-new-file");
    }


    @Test
    public void shouldUpdateHandlerSuffix() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-old-suffix")
                .formatter("the-formatter")
                .deployed();

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-new-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyUpdated().correctSuffix("the-new-suffix");
    }


    @Test
    public void shouldUpdateHandlerFormatter() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-old-formatter")
                .deployed();

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-new-formatter\n");

        fixture.verifyUpdated().correctFormatter("the-new-formatter");
    }


    @Test
    public void shouldUpdateHandlerFileAndFormatter() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-old-file")
                .suffix("the-suffix")
                .formatter("the-old-formatter")
                .deployed();

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-new-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-new-formatter\n");

        fixture.verifyUpdated().correctFile("the-new-file");
        fixture.verifyUpdated().correctFormatter("the-new-formatter");
    }


    // TODO shouldRemoveHandlerWhenStateIsUndeployed
    // TODO shouldRemoveHandlerWhenManaged
    // TODO shouldAddConsoleHandler
    // TODO shouldAddCustomHandler

    // TODO shouldAddLoggerAndHandler
}
