package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LoggerAudit;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;

import javax.ws.rs.BadRequestException;

import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.deployer.model.LoggerCategory.ROOT;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.step;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.toModelNode;
import static com.github.t1.log.LogLevel.DEBUG;
import static com.github.t1.log.LogLevel.INFO;
import static com.github.t1.log.LogLevel.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.quality.Strictness.LENIENT;

@MockitoSettings(strictness = LENIENT)
class LoggerDeployerTest extends AbstractDeployerTests {
    @Test void shouldAddEmptyLoggers() {
        deployWithRootBundle(""
            + "loggers:\n");
    }

    @Test void shouldFailToAddLoggerWithoutItem() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "loggers:\n"
            + "  foo:\n"));

        assertThat(thrown).hasStackTraceContaining("incomplete loggers plan 'foo'");
    }

    @Test void shouldAddLogger() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n");

        fixture.verifyAdded();
    }


    @Test void shouldNotAddExistingLogger() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app").level(DEBUG).deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n");

        logger.verifyUnchanged();
    }


    @Test void shouldUpdateLogLevel() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG).deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: INFO\n");

        fixture.level(INFO).verifyUpdatedLogLevelFrom(DEBUG);
    }


    @Test void shouldFailToAddPluralHandlersAndSingularHandler() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "loggers:\n"
            + "  foo:\n"
            + "    handler: CONSOLE\n"
            + "    handlers: [FILE]\n"));

        assertThat(thrown).hasStackTraceContaining("Can't have 'handler' _and_ 'handlers'");
    }

    @Test void shouldFailToExplicitlySetUseParentHandlersOfRootLoggerToTrue() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "loggers:\n"
            + "  ROOT:\n"
            + "    level: DEBUG\n"
            + "    use-parent-handlers: true\n"
            + "    handlers: [CONSOLE, FILE]\n"));

        assertThat(thrown).hasStackTraceContaining("Can't set use-parent-handlers of ROOT");
    }

    @Test void shouldFailToExplicitlySetUseParentHandlersOfRootLoggerToFalse() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "loggers:\n"
            + "  ROOT:\n"
            + "    level: DEBUG\n"
            + "    use-parent-handlers: false\n"
            + "    handlers: [CONSOLE, FILE]\n"));

        assertThat(thrown).hasStackTraceContaining("Can't set use-parent-handlers of ROOT");
    }

    @Test void shouldNotImplicitlySetUseParentHandlersOfRootLogger() {
        deployWithRootBundle(""
            + "loggers:\n"
            + "  ROOT:\n"
            + "    level: DEBUG\n"
            + "    handlers: [CONSOLE, FILE]\n");

        verifyWriteAttribute(rootLoggerNode(), "level", "DEBUG");
        assertThat(boundary.audits.getAudits()).containsExactly(new LoggerAudit().setCategory(ROOT).change("level", "INFO", "DEBUG").changed());
    }

    @Test void shouldFailToUndeployRootLogger() {
        Throwable throwable = catchThrowable(() -> deployWithRootBundle(""
            + "loggers:\n"
            + "  ROOT:\n"
            + "    state: undeployed\n"));

        assertThat(throwable)
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("can't remove root logger");
    }

    @Test void shouldAddLoggerWithExplicitState() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    state: deployed\n");

        fixture.verifyAdded();
    }


    @Test void shouldAddLoggerWithDefaultLevel() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    state: deployed\n");

        logger.verifyAdded();
    }


    @Test void shouldAddLoggerWithDefaultVariableLevel() {
        givenConfiguredVariable("default.log-level", "WARN");
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app").level(WARN);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    state: deployed\n");

        logger.verifyAdded();
    }


    @Test void shouldAddLoggerWithOneHandler() {
        givenLogHandler(periodicRotatingFile, "FOO").level(DEBUG).deployed();
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .handler("FOO")
            .useParentHandlers(false);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    handler: FOO\n");

        logger.verifyAdded();
    }


    @Test void shouldAddLoggerWithTwoHandlers() {
        givenLogHandler(periodicRotatingFile, "FOO").level(DEBUG).deployed();
        givenLogHandler(periodicRotatingFile, "BAR").level(DEBUG).deployed();
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .handler("FOO")
            .handler("BAR")
            .useParentHandlers(false);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    handlers: [FOO,BAR]\n");

        logger.verifyAdded();
    }


    @Test void shouldAddLoggerWithExplicitUseParentHandlersTrue() {
        givenLogHandler(periodicRotatingFile, "FOO").level(DEBUG).deployed();
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .handler("FOO")
            .useParentHandlers(true);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    handler: FOO\n"
            + "    use-parent-handlers: true");

        logger.verifyAdded();
    }


    @Test void shouldAddLoggerWithExplicitUseParentHandlersFalse() {
        givenLogHandler(periodicRotatingFile, "FOO").level(DEBUG).deployed();
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .handler("FOO")
            .useParentHandlers(false);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    handler: FOO\n"
            + "    use-parent-handlers: false");

        logger.verifyAdded();
    }


    @Test void shouldAddLoggerWithoutLogHandlersButExplicitUseParentHandlersTrue() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .useParentHandlers(true);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    use-parent-handlers: true");

        logger.verifyAdded();
    }


    @Test void shouldFailToAddLoggerWithoutLogHandlersButExplicitUseParentHandlersFalse() {
        givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .useParentHandlers(false)
            .deployed();

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer:\n"
            + "    level: DEBUG\n"
            + "    use-parent-handlers: false"));

        assertThat(thrown).hasStackTraceContaining("Can't set use-parent-handlers of [com.github.t1.deployer]"
            + " to false when there are no handlers");
    }


    @Test void shouldNotUpdateLoggerWithHandlerAndUseParentHandlersFalseToFalse() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .handler("FOO")
            .useParentHandlers(false)
            .deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    handler: FOO\n"
            + "    use-parent-handlers: false\n");

        logger.verifyUnchanged();
    }


    @Test void shouldUpdateLoggerWithHandlerAndUseParentHandlersFalseToTrue() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .handler("FOO")
            .useParentHandlers(false)
            .deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    handler: FOO\n"
            + "    use-parent-handlers: true\n");

        logger.useParentHandlers(true).verifyUpdatedUseParentHandlersFrom(false);
    }


    @Test void shouldUpdateLoggerWithHandlerAndUseParentHandlersTrueToFalse() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .handler("FOO")
            .useParentHandlers(true)
            .deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    handler: FOO\n"
            + "    use-parent-handlers: false\n");

        logger.useParentHandlers(false).verifyUpdatedUseParentHandlersFrom(true);
    }


    @Test void shouldUpdateLoggerWithoutHandlerAndWithUseParentHandlersFalseToTrue() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .useParentHandlers(false)
            .deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    use-parent-handlers: true\n");

        logger.useParentHandlers(true).verifyUpdatedUseParentHandlersFrom(false);
    }


    @Test void shouldFailToUpdateLoggerWithoutHandlerAndWithUseParentHandlersTrueToFalse() {
        givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .useParentHandlers(true)
            .deployed();

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer:\n"
            + "    level: DEBUG\n"
            + "    use-parent-handlers: false\n"));

        assertThat(thrown).hasStackTraceContaining("Can't set use-parent-handlers of [com.github.t1.deployer]"
            + " to false when there are no handlers");
    }


    @Test void shouldAddHandler() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .handler("FOO")
            .deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    handlers: [FOO,BAR]\n");


        assertThat(capturedOperations())
            .haveExactly(1, step(toModelNode(""
                + "{\n"
                + logger.loggerAddress()
                + "    'operation' => 'write-attribute',\n"
                + "    'name' => 'use-parent-handlers',\n"
                + "    'value' => false\n"
                + "}")))
            .haveExactly(1, step(toModelNode(""
                + "{\n"
                + logger.loggerAddress()
                + "    'operation' => 'add-handler',\n"
                + "    'name' => 'BAR'\n"
                + "}")));
        assertThat(boundary.audits.getAudits()).contains(
            new LoggerAudit().setCategory(logger.getCategory())
                .change("use-parent-handlers", true, false)
                .change("handlers", null, "[BAR]")
                .changed());
    }


    @Test void shouldRemoveHandler() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .handler("FOO")
            .handler("BAR")
            .deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    handlers: [FOO]\n");

        assertThat(capturedOperations())
            .haveExactly(1, step(toModelNode(""
                + "{\n"
                + logger.loggerAddress()
                + "    'operation' => 'write-attribute',\n"
                + "    'name' => 'use-parent-handlers',\n"
                + "    'value' => false\n"
                + "}")))
            .haveExactly(1, step(toModelNode(""
                + "{\n"
                + logger.loggerAddress()
                + "    'operation' => 'remove-handler',\n"
                + "    'name' => 'BAR'\n"
                + "}")));
        assertThat(boundary.audits.getAudits()).contains(
            new LoggerAudit().setCategory(logger.getCategory())
                .change("use-parent-handlers", true, false)
                .change("handlers", "[BAR]", null)
                .changed());
    }


    @Test void shouldRemoveExistingLoggerWhenStateIsUndeployed() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app")
            .level(DEBUG)
            .useParentHandlers(true)
            .handler("FOO")
            .handler("BAR")
            .deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    state: undeployed\n");

        fixture.verifyRemoved();
    }


    @Test void shouldRemoveNonExistingLoggerWhenStateIsUndeployed() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app:\n"
            + "    level: DEBUG\n"
            + "    state: undeployed\n");

        logger.verifyUnchanged();
    }

    @Test void shouldRemoveLoggerWhenManaged() {
        LoggerFixture app1 = givenLogger("com.github.t1.deployer.app1").level(DEBUG).deployed();
        LoggerFixture app2 = givenLogger("com.github.t1.deployer.app2").level(DEBUG).handler("FOO").deployed();
        givenManaged("loggers");

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app1:\n"
            + "    level: DEBUG\n");

        app1.verifyUnchanged();
        app2.verifyRemoved();
    }

    @Test void shouldRemoveLoggerWhenAllManaged() {
        LoggerFixture app1 = givenLogger("com.github.t1.deployer.app1").level(DEBUG).deployed();
        LoggerFixture app2 = givenLogger("com.github.t1.deployer.app2").level(DEBUG).deployed();
        givenManaged("all");

        deployWithRootBundle(""
            + "loggers:\n"
            + "  com.github.t1.deployer.app1:\n"
            + "    level: DEBUG\n");

        app1.verifyUnchanged();
        app2.verifyRemoved();
    }

    @Test void shouldIgnorePinnedLoggerWhenManaged() {
        givenManaged("all");
        LoggerFixture foo = givenLogger("FOO").level(DEBUG).deployed();
        LoggerFixture bar = givenLogger("BAR").deployed().pinned();
        LoggerFixture baz = givenLogger("BAZ").deployed();

        deployWithRootBundle(""
            + "loggers:\n"
            + "  FOO:\n"
            + "    level: DEBUG\n");

        foo.verifyUnchanged();
        bar.verifyUnchanged();
        baz.verifyRemoved();
    }

    @Test void shouldFailToDeployPinnedLogger() {
        givenLogger("FOO").deployed().pinned();

        Throwable thrown = catchThrowable(() ->
            deployWithRootBundle(""
                + "loggers:\n"
                + "  FOO:\n"
                + "    level: DEBUG\n"));

        assertThat(thrown)
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("resource is pinned: logger:deployed:FOO:");
    }
}
