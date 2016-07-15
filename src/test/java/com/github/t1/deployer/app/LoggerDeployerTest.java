package com.github.t1.deployer.app;

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

        assertThat(thrown.getCause()).hasMessageContaining("no config in logger 'foo'");
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

        assertThat(thrown.getCause())
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

        assertThat(thrown.getCause())
                .hasMessageContaining("Can't set use-parent-handlers to false when there are no handlers");
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
}
