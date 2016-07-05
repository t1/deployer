package com.github.t1.deployer.app;

import com.github.t1.problem.WebApplicationApplicationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class LoggerDeployerTest extends AbstractDeployerTest {
    @Test
    public void shouldAddEmptyLoggers() {
        deployer.run(""
                + "loggers:\n");
    }

    @Test
    public void shouldFailToAddLoggerWithoutItem() {
        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "loggers:\n"
                + "  foo:\n"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("no config in loggers:foo");
    }

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
    public void shouldAddLoggerWithoutLevel() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app");

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    state: deployed\n");

        logger.verifyAdded(audits);
    }


    @Test
    public void shouldAddLoggerWithOneHandler() {
        givenLogHandler(periodicRotatingFile, "FOO").level(DEBUG).deployed();
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(false);

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handler: FOO\n");

        logger.verifyAdded(audits);
    }


    @Test
    public void shouldAddLoggerWithTwoHandlers() {
        givenLogHandler(periodicRotatingFile, "FOO").level(DEBUG).deployed();
        givenLogHandler(periodicRotatingFile, "BAR").level(DEBUG).deployed();
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .handler("BAR")
                .useParentHandlers(false);

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handlers: [FOO,BAR]\n");

        logger.verifyAdded(audits);
    }


    @Test
    public void shouldAddLoggerWithExplicitUseParentHandlersTrue() {
        givenLogHandler(periodicRotatingFile, "FOO").level(DEBUG).deployed();
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(true);

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handler: FOO\n"
                + "    use-parent-handlers: true");

        logger.verifyAdded(audits);
    }


    @Test
    public void shouldAddLoggerWithExplicitUseParentHandlersFalse() {
        givenLogHandler(periodicRotatingFile, "FOO").level(DEBUG).deployed();
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(false);

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handler: FOO\n"
                + "    use-parent-handlers: false");

        logger.verifyAdded(audits);
    }


    @Test
    public void shouldAddLoggerWithoutLogHandlersButExplicitUseParentHandlersTrue() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .useParentHandlers(true);

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    use-parent-handlers: true");

        logger.verifyAdded(audits);
    }


    @Test
    public void shouldFailToAddLoggerWithoutLogHandlersButExplicitUseParentHandlersFalse() {
        givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .useParentHandlers(false)
                .deployed();

        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    use-parent-handlers: false"));

        assertThat(thrown).isInstanceOf(WebApplicationApplicationException.class)
                          .hasMessageContaining("Can't set use-parent-handlers to false when there are no handlers");
    }


    @Test
    public void shouldNotUpdateLoggerWithHandlerAndUseParentHandlersFalseToFalse() {
        givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(false)
                .deployed();

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handler: FOO\n"
                + "    use-parent-handlers: false\n");

        assertThat(audits.asList()).isEmpty();
    }


    @Test
    public void shouldUpdateLoggerWithHandlerAndUseParentHandlersFalseToTrue() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(false)
                .deployed();

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handler: FOO\n"
                + "    use-parent-handlers: true\n");

        logger.useParentHandlers(true).verifyUpdatedUseParentHandlers(audits);
    }


    @Test
    public void shouldUpdateLoggerWithHandlerAndUseParentHandlersTrueToFalse() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(true)
                .deployed();

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handler: FOO\n"
                + "    use-parent-handlers: false\n");

        logger.useParentHandlers(false).verifyUpdatedUseParentHandlers(audits);
    }


    @Test
    public void shouldUpdateLoggerWithoutHandlerAndWithUseParentHandlersFalseToTrue() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .useParentHandlers(false)
                .deployed();

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    use-parent-handlers: true\n");

        logger.useParentHandlers(true).verifyUpdatedUseParentHandlers(audits);
    }


    @Test
    public void shouldFailToUpdateLoggerWithoutHandlerAndWithUseParentHandlersTrueToFalse() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .useParentHandlers(true)
                .deployed();

        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    use-parent-handlers: false\n"));

        assertThat(thrown).hasMessageContaining("Can't set use-parent-handlers to false when there are no handlers");
    }


    @Test
    public void shouldAddLoggerHandler() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(false)
                .deployed();

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handlers: [FOO,BAR]\n");

        logger.verifyAddedHandlers(audits, "BAR");
    }


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
    public void shouldAddEmptyLogHandlers() {
        deployer.run(""
                + "log-handlers:\n");
    }

    @Test
    public void shouldFailToAddLogHandlersWithoutItem() {
        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "log-handlers:\n"
                + "  foo:\n"));

        assertThat(thrown).isInstanceOf(NullPointerException.class)
                          .hasMessageContaining("no config in log-handlers:foo");
    }

    @Test
    public void shouldAddLogHandlerWithDefaultType() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandler() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandlerWithDefaultLevel() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .suffix("the-suffix")
                .format("the-format");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.file("FOO").verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandlerWithDefaultFile() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(DEBUG)
                .suffix("the-suffix")
                .format("the-format");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: DEBUG\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.file("FOO").verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandlerWithDefaultSuffix() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .format("the-format");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    format: the-format\n");

        fixture.suffix(".yyyy-MM-dd").verifyAdded(audits);
    }


    @Test
    public void shouldAddHandlerWithDefaultTypeAndFileAndSuffix() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .format("the-format");

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    format: the-format\n");

        fixture.file("FOO").suffix(".yyyy-MM-dd").verifyAdded(audits);
    }


    @Test
    public void shouldNotAddExistingHandler() {
        givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        // #after(): not added/updated
        assertThat(audits.asList()).isEmpty();
    }


    @Test
    public void shouldUpdateHandlerLevel() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(DEBUG)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.level(ALL).verifyUpdatedLogLevel(audits);
    }


    @Test
    public void shouldUpdateHandlerFile() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-old-file")
                .suffix("the-suffix")
                .format("the-format")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-new-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.file("the-new-file").verifyUpdatedFile(audits);
    }


    @Test
    public void shouldUpdateHandlerSuffix() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-old-suffix")
                .format("the-format")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-new-suffix\n"
                + "    format: the-format\n");

        fixture.suffix("the-new-suffix").verifyUpdatedSuffix(audits);
    }


    @Test
    public void shouldUpdateHandlerFormat() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-old-format")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-new-format\n");

        fixture.format("the-new-format").verifyUpdatedFormat(audits);
    }


    @Test
    public void shouldUpdateHandlerFileAndFormat() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-old-file")
                .suffix("the-suffix")
                .format("the-old-format")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-new-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-new-format\n");

        fixture.file("the-new-file").format("the-new-format");
        fixture.verifyLogHandler().correctFile(fixture.getFile());
        fixture.verifyLogHandler().correctFormat(fixture.getFormat());
        assertThat(audits.asList()).isEmpty();
    }


    // TODO shouldRemoveHandlerWhenStateIsUndeployed
    // TODO shouldRemoveHandlerWhenManaged
    // TODO shouldAddConsoleHandler
    // TODO shouldAddCustomHandler

    // TODO shouldAddLoggerAndHandler
}
