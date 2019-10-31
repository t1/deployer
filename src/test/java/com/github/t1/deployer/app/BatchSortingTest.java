package com.github.t1.deployer.app;

import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;

import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.log.LogLevel.ALL;
import static com.github.t1.log.LogLevel.DEBUG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.mockito.quality.Strictness.LENIENT;

@MockitoSettings(strictness = LENIENT)
class BatchSortingTest extends AbstractDeployerTests {
    @Test void shouldAddLoggerAndHandler() {
        LogHandlerFixture handler = givenLogHandler(periodicRotatingFile, "FOO")
            .level(ALL)
            .file("the-file")
            .suffix("the-suffix")
            .format("the-format");
        LoggerFixture logger = givenLogger("foo").level(DEBUG).handler("FOO").useParentHandlers(false);

        deployWithRootBundle(""
            + "log-handlers:\n"
            + "  FOO:\n"
            + "    type: periodic-rotating-file\n"
            + "    level: ALL\n"
            + "    file: the-file\n"
            + "    suffix: the-suffix\n"
            + "    format: the-format\n"
            + "loggers:\n"
            + "  foo:\n"
            + "    level: DEBUG\n"
            + "    handler: FOO\n");

        handler.verifyAdded();
        logger.verifyAdded();
        assertThat(steps()).hasSize(2);
        assertThat(steps().get(0).get(ADDRESS).toString())
            .as("add handler first")
            .contains("\"periodic-rotating-file-handler\" => \"FOO\"");
        assertThat(steps().get(1).get(ADDRESS).toString())
            .as("add logger last")
            .contains("\"logger\" => \"foo\"");
    }

    @Test void shouldRemoveLoggerAndHandler() {
        givenManaged("all");
        LogHandlerFixture handler = givenLogHandler(periodicRotatingFile, "FOO")
            .level(ALL)
            .file("the-file")
            .suffix("the-suffix")
            .format("the-format")
            .deployed();
        LoggerFixture logger = givenLogger("foo").level(DEBUG).handler("FOO").useParentHandlers(false).deployed();

        deployWithRootBundle("---");

        handler.verifyRemoved();
        logger.verifyRemoved();
        assertThat(steps()).hasSize(2);
        assertThat(steps().get(0).get(ADDRESS).toString())
            .as("remove logger first")
            .contains("\"logger\" => \"foo\"");
        assertThat(steps().get(1).get(ADDRESS).toString())
            .as("remove handler last")
            .contains("\"periodic-rotating-file-handler\" => \"FOO\"");
    }
}
