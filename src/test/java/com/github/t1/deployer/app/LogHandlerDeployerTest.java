package com.github.t1.deployer.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class LogHandlerDeployerTest extends AbstractDeployerTest {
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

        assertThat(thrown.getCause()).hasMessageContaining("no config in log-handler 'foo'");
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
    public void shouldAddLogHandlerWithFormatter() {
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
    public void shouldFailToParsePlanWithLogHandlerWithNeitherFormatNorFormatter() {
        Throwable throwable = catchThrowable(() -> deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"));

        assertThat(throwable.getCause().getCause())
                .hasMessage("log-handler [FOO] must either have a format or a formatter");
    }


    @Test
    public void shouldFailToParsePlanWithLogHandlerWithBothFormatAndFormatter() {
        Throwable throwable = catchThrowable(() -> deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n"
                + "    formatter: the-formatter\n"));

        assertThat(throwable.getCause().getCause())
                .hasMessage("log-handler [FOO] must either have a format or a formatter");
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
        assertThat(audits.getAudits()).isEmpty();
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

        fixture.verifyChange("level", DEBUG, ALL);
        fixture.verifyChanged(audits);
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

        fixture.verifyChange("file", "the-old-file", "the-new-file");
        fixture.verifyChanged(audits);
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

        fixture.verifyChange("suffix", "the-old-suffix", "the-new-suffix");
        fixture.verifyChanged(audits);
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

        fixture.verifyChange("format", "the-old-format", "the-new-format");
        fixture.verifyChanged(audits);
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
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-new-formatter\n");

        fixture.verifyWriteAttribute("named-formatter", "the-new-formatter");
        fixture.expectChange("formatter", "the-old-formatter", "the-new-formatter");
        fixture.verifyChanged(audits);
    }


    @Test
    public void shouldUpdateHandlerFormatToFormatter() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
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
                + "    formatter: the-formatter\n");

        fixture.verifyChange("format", "the-format", null);
        fixture.verifyWriteAttribute("named-formatter", "the-formatter");
        fixture.expectChange("formatter", null, "the-formatter");
        fixture.verifyChanged(audits);
    }


    @Test
    public void shouldUpdateHandlerFormatterToFormat() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.verifyWriteAttribute("format", "the-format");
        fixture.expectChange("format", null, "the-format");
        fixture.verifyWriteAttribute("named-formatter", null);
        fixture.expectChange("formatter", "the-formatter", null);
        fixture.verifyChanged(audits);
    }


    @Test
    public void shouldUpdateHandlerFileAndSuffix() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-old-file")
                .suffix("the-old-suffix")
                .format("the-format")
                .deployed();

        Audits audits = deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-new-file\n"
                + "    suffix: the-new-suffix\n"
                + "    format: the-format\n");

        fixture.verifyChange("file", "the-old-file", "the-new-file");
        fixture.verifyChange("suffix", "the-old-suffix", "the-new-suffix");
        fixture.verifyChanged(audits);
    }


    @Test
    public void shouldRemoveHandlerWhenStateIsUndeployed() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
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
                + "    format: the-format\n"
                + "    state: undeployed\n");

        fixture.verifyRemoved(audits);
    }


    @Test
    public void shouldNotRemoveUndeployedHandlerWhenStateIsUndeployed() {
        givenLogHandler(periodicRotatingFile, "FOO")
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
                + "    format: the-format\n"
                + "    state: undeployed\n");

        assertThat(audits.getAudits()).isEmpty();
    }


    // TODO shouldRemoveHandlerWhenManaged
    // TODO shouldAddConsoleHandler
    // TODO shouldAddCustomHandler

    // TODO shouldAddLoggerAndHandler
}
