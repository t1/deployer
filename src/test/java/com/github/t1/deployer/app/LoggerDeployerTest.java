package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LoggerAudit;
import com.github.t1.problem.WebApplicationApplicationException;
import org.junit.Test;

import static com.github.t1.deployer.model.LogHandlerType.*;
import static com.github.t1.deployer.model.LoggerCategory.*;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

public class LoggerDeployerTest extends AbstractDeployerTests {
    @Test
    public void shouldAddEmptyLoggers() {
        deploy(""
                + "loggers:\n");
    }

    @Test
    public void shouldFailToAddLoggerWithoutItem() {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "loggers:\n"
                + "  foo:\n"));

        assertThat(thrown).hasStackTraceContaining("incomplete loggers plan 'foo'");
    }

    @Test
    public void shouldAddLogger() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldNotAddExistingLogger() {
        givenLogger("com.github.t1.deployer.app").level(DEBUG).deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n");

        // #after(): no add nor update
        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldUpdateLogLevel() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG).deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: INFO\n");

        fixture.level(INFO).verifyUpdatedLogLevelFrom(DEBUG, audits);
    }


    @Test
    public void shouldFailToAddPluralHandlersAndSingularHandler() {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "loggers:\n"
                + "  foo:\n"
                + "    handler: CONSOLE\n"
                + "    handlers: [FILE]\n"));

        assertThat(thrown).hasStackTraceContaining("Can't have 'handler' _and_ 'handlers'");
    }

    @Test
    public void shouldFailToExplicitlySetUseParentHandlersOfRootLoggerToTrue() {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "loggers:\n"
                + "  ROOT:\n"
                + "    level: DEBUG\n"
                + "    use-parent-handlers: true\n"
                + "    handlers: [CONSOLE, FILE]\n"));

        assertThat(thrown).hasStackTraceContaining("Can't set use-parent-handlers of ROOT");
    }

    @Test
    public void shouldFailToExplicitlySetUseParentHandlersOfRootLoggerToFalse() {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "loggers:\n"
                + "  ROOT:\n"
                + "    level: DEBUG\n"
                + "    use-parent-handlers: false\n"
                + "    handlers: [CONSOLE, FILE]\n"));

        assertThat(thrown).hasStackTraceContaining("Can't set use-parent-handlers of ROOT");
    }

    @Test
    public void shouldNotImplicitlySetUseParentHandlersOfRootLogger() {
        Audits audits = deploy(""
                + "loggers:\n"
                + "  ROOT:\n"
                + "    level: DEBUG\n"
                + "    handlers: [CONSOLE, FILE]\n");

        verifyWriteAttribute(rootLoggerNode(), "level", "DEBUG");
        assertThat(audits.getAudits()).containsExactly(LoggerAudit.of(ROOT).change("level", "INFO", "DEBUG").changed());
    }

    @Test
    public void shouldFailToUndeployRootLogger() {
        Throwable throwable = catchThrowable(() -> deploy(""
                + "loggers:\n"
                + "  ROOT:\n"
                + "    state: undeployed\n"));

        assertThat(throwable)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("can't remove root logger");
    }

    @Test
    public void shouldAddLoggerWithExplicitState() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: deployed\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddLoggerWithDefaultLevel() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app").level(DEBUG);

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    state: deployed\n");

        logger.verifyAdded(audits);
    }


    @Test
    public void shouldAddLoggerWithDefaultVariableLevel() {
        givenConfiguredVariable("default.log-level", "WARN");
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app").level(WARN);

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Throwable thrown = catchThrowable(() -> deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer:\n"
                + "    level: DEBUG\n"
                + "    use-parent-handlers: false"));

        assertThat(thrown).hasStackTraceContaining("Can't set use-parent-handlers of [com.github.t1.deployer]"
                + " to false when there are no handlers");
    }


    @Test
    public void shouldNotUpdateLoggerWithHandlerAndUseParentHandlersFalseToFalse() {
        givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(false)
                .deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handler: FOO\n"
                + "    use-parent-handlers: false\n");

        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldUpdateLoggerWithHandlerAndUseParentHandlersFalseToTrue() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(false)
                .deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handler: FOO\n"
                + "    use-parent-handlers: true\n");

        logger.useParentHandlers(true).verifyUpdatedUseParentHandlersFrom(false, audits);
    }


    @Test
    public void shouldUpdateLoggerWithHandlerAndUseParentHandlersTrueToFalse() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .useParentHandlers(true)
                .deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handler: FOO\n"
                + "    use-parent-handlers: false\n");

        logger.useParentHandlers(false).verifyUpdatedUseParentHandlersFrom(true, audits);
    }


    @Test
    public void shouldUpdateLoggerWithoutHandlerAndWithUseParentHandlersFalseToTrue() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .useParentHandlers(false)
                .deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    use-parent-handlers: true\n");

        logger.useParentHandlers(true).verifyUpdatedUseParentHandlersFrom(false, audits);
    }


    @Test
    public void shouldFailToUpdateLoggerWithoutHandlerAndWithUseParentHandlersTrueToFalse() {
        givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .useParentHandlers(true)
                .deployed();

        Throwable thrown = catchThrowable(() -> deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer:\n"
                + "    level: DEBUG\n"
                + "    use-parent-handlers: false\n"));

        assertThat(thrown).hasStackTraceContaining("Can't set use-parent-handlers of [com.github.t1.deployer]"
                + " to false when there are no handlers");
    }


    @Test
    public void shouldAddHandler() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handlers: [FOO,BAR]\n");


        assertThat(captureOperations())
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
        assertThat(audits.getAudits()).contains(
                LoggerAudit.of(logger.getCategory())
                           .change("use-parent-handlers", true, false)
                           .change("handlers", null, "[BAR]")
                           .changed());
    }


    @Test
    public void shouldRemoveHandler() {
        LoggerFixture logger = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .handler("FOO")
                .handler("BAR")
                .deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    handlers: [FOO]\n");

        assertThat(captureOperations())
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
        assertThat(audits.getAudits()).contains(
                LoggerAudit.of(logger.getCategory())
                           .change("use-parent-handlers", true, false)
                           .change("handlers", "[BAR]", null)
                           .changed());
    }


    @Test
    public void shouldRemoveExistingLoggerWhenStateIsUndeployed() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app")
                .level(DEBUG)
                .useParentHandlers(true)
                .handler("FOO")
                .handler("BAR")
                .deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    state: undeployed\n");

        fixture.verifyRemoved(audits);
    }


    @Test
    public void shouldRemoveNonExistingLoggerWhenStateIsUndeployed() {
        givenLogger("com.github.t1.deployer.app").level(DEBUG);

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: undeployed\n");

        // #after(): not undeployed
        assertThat(audits.getAudits()).isEmpty();
    }

    @Test
    public void shouldRemoveLoggerWhenManaged() {
        givenLogger("com.github.t1.deployer.app1").level(DEBUG).deployed();
        LoggerFixture app2 = givenLogger("com.github.t1.deployer.app2").level(DEBUG).handler("FOO").deployed();
        givenManaged("loggers");

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app1:\n"
                + "    level: DEBUG\n");

        // #after(): app1 not undeployed
        app2.verifyRemoved(audits);
    }

    @Test
    public void shouldRemoveLoggerWhenAllManaged() {
        givenLogger("com.github.t1.deployer.app1").level(DEBUG).deployed();
        LoggerFixture app2 = givenLogger("com.github.t1.deployer.app2").level(DEBUG).deployed();
        givenManaged("all");

        Audits audits = deploy(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app1:\n"
                + "    level: DEBUG\n");

        // #after(): app1 not undeployed
        app2.verifyRemoved(audits);
    }

    @Test
    public void shouldIgnorePinnedLoggerWhenManaged() {
        givenManaged("all");
        givenLogger("FOO").level(DEBUG).deployed();
        givenLogger("BAR").deployed().pinned();
        LoggerFixture baz = givenLogger("BAZ").deployed();

        Audits audits = deploy(""
                + "loggers:\n"
                + "  FOO:\n"
                + "    level: DEBUG\n");

        baz.verifyRemoved(audits);
    }

    @Test
    public void shouldFailToDeployPinnedLogger() {
        givenLogger("FOO").deployed().pinned();

        Throwable thrown = catchThrowable(() ->
                deploy(""
                        + "loggers:\n"
                        + "  FOO:\n"
                        + "    level: DEBUG\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("resource is pinned: logger:deployed:FOO:");
    }
}
