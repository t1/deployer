package com.github.t1.deployer.app;

import org.junit.Test;

import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.log.LogLevel.ALL;
import static com.github.t1.log.LogLevel.DEBUG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;

public class BatchSortingTest extends AbstractDeployerTests {
    @Test
    public void shouldAddLoggerAndHandler() {
        LogHandlerFixture handler = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format");
        LoggerFixture logger = givenLogger("foo").level(DEBUG).handler("FOO").useParentHandlers(false);

        Audits audits = deploy(""
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

        handler.verifyAdded(audits);
        logger.verifyAdded(audits);
        assertThat(steps()).hasSize(2);
        assertThat(steps().get(0).get(ADDRESS).toString())
                .as("add handler first")
                .contains("\"periodic-rotating-file-handler\" => \"FOO\"");
        assertThat(steps().get(1).get(ADDRESS).toString())
                .as("add logger last")
                .contains("\"logger\" => \"foo\"");
    }

    @Test
    public void shouldRemoveLoggerAndHandler() {
        givenManaged("all");
        LogHandlerFixture handler = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format")
                .deployed();
        LoggerFixture logger = givenLogger("foo").level(DEBUG).handler("FOO").useParentHandlers(false).deployed();

        Audits audits = deploy("---");

        handler.verifyRemoved(audits);
        logger.verifyRemoved(audits);
        assertThat(steps()).hasSize(2);
        assertThat(steps().get(0).get(ADDRESS).toString())
                .as("remove logger first")
                .contains("\"logger\" => \"foo\"");
        assertThat(steps().get(1).get(ADDRESS).toString())
                .as("remove handler last")
                .contains("\"periodic-rotating-file-handler\" => \"FOO\"");
    }
}
