package com.github.t1.deployer.app;

import com.github.t1.problem.WebApplicationApplicationException;
import org.junit.Test;

import static com.github.t1.deployer.model.LogHandlerType.console;
import static com.github.t1.deployer.model.LogHandlerType.custom;
import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.deployer.tools.Tools.toStringOrNull;
import static com.github.t1.log.LogLevel.ALL;
import static com.github.t1.log.LogLevel.DEBUG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class LogHandlerDeployerTest extends AbstractDeployerTests {
    @Test
    public void shouldAddEmptyLogHandlers() {
        deploy(""
                + "log-handlers:\n");
    }

    @Test
    public void shouldAddLogHandlersWithAllDefaults() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n");

        fixture.verifyAdded(audits);
    }

    @Test
    public void shouldAddLogHandlerWithEncoding() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .encoding("US-ASCII");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    encoding: US-ASCII\n");

        fixture.verifyAdded(audits);
    }

    @Test
    public void shouldAddLogHandlerWithDefaultType() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddConsoleHandler() {
        LogHandlerFixture fixture = givenLogHandler(console, "FOO")
                .level(ALL)
                .format("the-format");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    type: console\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddLogHandlerWithConfiguredDefaultType() {
        givenConfiguredVariable("default.log-handler-type", "console");
        LogHandlerFixture fixture = givenLogHandler(console, "FOO")
                .level(ALL)
                .format("the-format");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddLogHandlerWithDefaultFormat() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddLogHandlerWithConfiguredDefaultFormat() {
        givenConfiguredVariable("default.log-format", "the-format");
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddLogHandlerWithFormatter() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddLogHandlerWithConfiguredDefaultFormatter() {
        givenConfiguredVariable("default.log-formatter", "the-formatter");
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n");

        fixture.verifyAdded(audits);
    }

    @Test
    public void shouldAddLogHandlerWithConfiguredDefaultFormatAndFormatter() {
        givenConfiguredVariable("default.log-format", "the-format");
        givenConfiguredVariable("default.log-formatter", "the-formatter");
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldFailToParsePlanWithLogHandlerWithBothFormatAndFormatter() {
        Throwable throwable = catchThrowable(() -> deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n"
                + "    formatter: the-formatter\n"));

        assertThat(throwable).hasStackTraceContaining("log-handler [FOO] can't have both a format and a formatter");
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandler() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandlerWithDefaultLevel() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .suffix("the-suffix")
                .format("the-format");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandlerWithDefaultFile() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(DEBUG)
                .suffix("the-suffix")
                .format("the-format");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: DEBUG\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandlerWithDefaultSuffix() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .format("the-format");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandlerWithConfiguredDefaultSuffix() {
        givenConfiguredVariable("default.log-file-suffix", "the-default-suffix");
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .format("the-format")
                .suffix("the-default-suffix");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldAddHandlerWithDefaultTypeAndFileAndSuffix() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .format("the-format");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    format: the-format\n");

        fixture.verifyAdded(audits);
    }


    @Test
    public void shouldNotAddExistingHandler() {
        givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .format("the-format")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-new-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        verifyWriteAttribute(fixture.logHandlerAddressNode(), "file.path", toStringOrNull("the-new-file"));
        fixture.expectChange("file", "the-old-file", "the-new-file");
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-new-suffix\n"
                + "    format: the-format\n");

        fixture.verifyChange("suffix", "the-old-suffix", "the-new-suffix");
        fixture.verifyChanged(audits);
    }


    @Test
    public void shouldUpdateHandlerEncoding() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .encoding("the-old-encoding")
                .format("the-format")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    file: the-file\n"
                + "    encoding: the-new-encoding\n"
                + "    format: the-format\n");

        fixture.verifyChange("encoding", "the-old-encoding", "the-new-encoding");
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-new-format\n");

        verifyWriteAttribute(fixture.logHandlerAddressNode(), "formatter", toStringOrNull("the-new-format"));
        fixture.expectChange("format", "the-old-format", "the-new-format");
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-new-formatter\n");

        verifyWriteAttribute(fixture.logHandlerAddressNode(), "named-formatter", toStringOrNull("the-new-formatter"));
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        verifyWriteAttribute(fixture.logHandlerAddressNode(), "formatter", toStringOrNull(null));
        fixture.expectChange("format", "the-format", null);
        verifyWriteAttribute(fixture.logHandlerAddressNode(), "named-formatter", toStringOrNull("the-formatter"));
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        verifyWriteAttribute(fixture.logHandlerAddressNode(), "formatter", toStringOrNull("the-format"));
        fixture.expectChange("format", null, "the-format");
        verifyWriteAttribute(fixture.logHandlerAddressNode(), "named-formatter", toStringOrNull(null));
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-new-file\n"
                + "    suffix: the-new-suffix\n"
                + "    format: the-format\n");

        verifyWriteAttribute(fixture.logHandlerAddressNode(), "file.path", toStringOrNull("the-new-file"));
        fixture.expectChange("file", "the-old-file", "the-new-file");
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n"
                + "    state: undeployed\n");

        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldAddCustomHandlerWithoutProperties() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .class_("org.foo.MyHandler");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n");

        fixture.verifyAdded(audits);
    }

    @Test
    public void shouldFailToAddCustomHandlerWithoutModule() {
        Throwable throwable = catchThrowable(() -> deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    class: org.foo.MyHandler\n"));

        assertThat(throwable).hasStackTraceContaining(
                "log-handler [FOO] is of type [custom], so it requires a 'module'");
    }

    @Test
    public void shouldFailToAddCustomHandlerWithoutClass() {
        Throwable throwable = catchThrowable(() -> deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"));

        assertThat(throwable).hasStackTraceContaining(
                "log-handler [FOO] is of type [custom], so it requires a 'class'");
    }

    @Test
    public void shouldAddCustomHandlerWithProperties() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .class_("org.foo.MyHandler")
                .level(ALL)
                .formatter("the-formatter")
                .property("foo", "bar")
                .property("foos", "bars");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n"
                + "    level: ALL\n"
                + "    formatter: the-formatter\n"
                + "    properties:\n"
                + "      foo: bar\n"
                + "      foos: bars\n");

        fixture.verifyAdded(audits);
    }

    @Test
    public void shouldAddCustomLogHandlerWithFile() {
        LogHandlerFixture logHandler = givenLogHandler(custom, "CUSTOM")
                .module("foo")
                .class_("bar")
                .file("baz");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  CUSTOM:\n"
                + "    type: custom\n"
                + "    module: foo\n"
                + "    class: bar\n"
                + "    file: baz\n"
        );

        logHandler.verifyAdded(audits);
    }

    @Test
    public void shouldAddCustomLogHandlerWithSuffix() {
        LogHandlerFixture logHandler = givenLogHandler(custom, "CUSTOM")
                .module("foo")
                .class_("bar")
                .file("baz");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  CUSTOM:\n"
                + "    type: custom\n"
                + "    module: foo\n"
                + "    class: bar\n"
                + "    file: baz\n"
        );

        logHandler.verifyAdded(audits);
    }

    @Test
    public void shouldAddCustomHandlerWithEscapedAndUnescapedVariablesInAttributes() {
        givenConfiguredVariable("module", "bar");
        LogHandlerFixture logHandler = givenLogHandler(custom, "FOO")
                .module("bar")
                .class_("${class}");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: ${module}\n"
                + "    class: $${class}\n");

        logHandler.verifyAdded(audits);
    }

    @Test
    public void shouldUpdateCustomHandlerModule() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .class_("org.foo.MyHandler")
                .level(ALL)
                .formatter("the-formatter")
                .property("foo", "bar")
                .property("foos", "bars")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foos\n"
                + "    class: org.foo.MyHandler\n"
                + "    level: ALL\n"
                + "    formatter: the-formatter\n"
                + "    properties:\n"
                + "      foo: bar\n"
                + "      foos: bars\n");

        fixture.verifyChange("module", "org.foo", "org.foos").verifyChanged(audits);
    }

    @Test
    public void shouldUpdateCustomHandlerClass() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .class_("org.foo.MyHandler")
                .level(ALL)
                .formatter("the-formatter")
                .property("foo", "bar")
                .property("foos", "bars")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foos.MyHandler\n"
                + "    level: ALL\n"
                + "    formatter: the-formatter\n"
                + "    properties:\n"
                + "      foo: bar\n"
                + "      foos: bars\n");

        fixture.verifyChange("class", "org.foo.MyHandler", "org.foos.MyHandler").verifyChanged(audits);
    }

    @Test
    public void shouldAddFirstCustomHandlerProperty() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .level(ALL)
                .module("org.foo")
                .class_("org.foo.MyHandler")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n"
                + "    properties:\n"
                + "      foo: bar\n");

        fixture.verifyPutProperty("foo", "bar");
        fixture.expectChange("property:foo", null, "bar").verifyChanged(audits);
    }

    @Test
    public void shouldAddThirdCustomHandlerProperty() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .class_("org.foo.MyHandler")
                .level(ALL)
                .formatter("the-formatter")
                .property("foo", "bar")
                .property("foos", "bars")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n"
                + "    level: ALL\n"
                + "    formatter: the-formatter\n"
                + "    properties:\n"
                + "      foo: bar\n"
                + "      foos: bars\n"
                + "      bax: bbb");

        fixture.verifyPutProperty("bax", "bbb");
        fixture.expectChange("property:bax", null, "bbb").verifyChanged(audits);
    }

    @Test
    public void shouldAddCustomHandlerPropertyWithExpression() {
        givenConfiguredVariable("bars", "bar");
        givenConfiguredVariable("var", "val");
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .level(ALL)
                .class_("org.foo.MyHandler")
                .property("foo", "1")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n"
                + "    properties:\n"
                + "      foo: 1\n"
                + "      ${bars}: ${var}\n"
                + "      $${baz}: $${none}\n");

        fixture.verifyPutProperty("bar", "val");
        fixture.verifyPutProperty("${baz}", "${none}");
        fixture.expectChange("property:bar", null, "val")
               .expectChange("property:${baz}", null, "${none}")
               .verifyChanged(audits);
    }

    @Test
    public void shouldChangeCustomHandlerPropertyValue() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .class_("org.foo.MyHandler")
                .level(ALL)
                .formatter("the-formatter")
                .property("foo", "bar")
                .property("foos", "bars")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n"
                + "    level: ALL\n"
                + "    formatter: the-formatter\n"
                + "    properties:\n"
                + "      foo: bax\n"
                + "      foos: bars\n");

        fixture.verifyPutProperty("foo", "bax");
        fixture.expectChange("property:foo", "bar", "bax").verifyChanged(audits);
    }

    @Test
    public void shouldRemoveCustomHandlerProperty() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .class_("org.foo.MyHandler")
                .level(ALL)
                .formatter("the-formatter")
                .property("foo", "bar")
                .property("foos", "bars")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n"
                + "    level: ALL\n"
                + "    formatter: the-formatter\n"
                + "    properties:\n"
                + "      foo: bar\n");

        fixture.verifyRemoveProperty("foos");
        fixture.expectChange("property:foos", "bars", null).verifyChanged(audits);
    }

    @Test
    public void shouldChangeCustomHandlerPropertyKey() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .class_("org.foo.MyHandler")
                .level(ALL)
                .formatter("the-formatter")
                .property("foo", "bar")
                .property("foos", "bars")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n"
                + "    level: ALL\n"
                + "    formatter: the-formatter\n"
                + "    properties:\n"
                + "      bax: bar\n"
                + "      foos: bars\n");

        fixture.verifyRemoveProperty("foo");
        fixture.verifyPutProperty("bax", "bar");
        fixture.expectChange("property:bax", null, "bar")
               .expectChange("property:foo", "bar", null)
               .verifyChanged(audits);
    }

    @Test
    public void shouldRemoveCustomHandler() {
        LogHandlerFixture fixture = givenLogHandler(custom, "FOO")
                .module("org.foo")
                .class_("org.foo.MyHandler")
                .level(ALL)
                .formatter("the-formatter")
                .property("foo", "bar")
                .property("foos", "bars")
                .deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: custom\n"
                + "    module: org.foo\n"
                + "    class: org.foo.MyHandler\n"
                + "    level: ALL\n"
                + "    formatter: the-formatter\n"
                + "    properties:\n"
                + "      foo: bar\n"
                + "      foos: bars\n"
                + "    state: undeployed");

        fixture.verifyRemoved(audits);
    }


    @Test
    public void shouldRemoveLogHandlerWhenManaged() {
        givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("foo.log")
                .suffix("the-suffix")
                .format("the-format")
                .deployed();
        LogHandlerFixture bar = givenLogHandler(periodicRotatingFile, "BAR").deployed();
        givenManaged("log-handlers");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        // #after(): foo not undeployed
        bar.verifyRemoved(audits);
    }

    @Test
    public void shouldRemoveLogHandlerWhenAllManaged() {
        givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("foo.log")
                .suffix("the-suffix")
                .format("the-format")
                .deployed();
        LogHandlerFixture bar = givenLogHandler(periodicRotatingFile, "BAR").deployed();
        givenManaged("all");

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        // #after(): foo not undeployed
        bar.verifyRemoved(audits);
    }

    @Test
    public void shouldIgnorePinnedLogHandlerWhenManaged() {
        givenManaged("all");
        givenLogHandler(periodicRotatingFile, "FOO").level(ALL).formatter("foo").file("foo.log").deployed();
        givenLogHandler(periodicRotatingFile, "BAR").deployed().pinned();
        LogHandlerFixture baz = givenLogHandler(periodicRotatingFile, "BAZ").deployed();

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    formatter: foo");

        baz.verifyRemoved(audits);
    }

    @Test
    public void shouldFailToDeployPinnedLogHandler() {
        givenLogHandler(periodicRotatingFile, "FOO").deployed().pinned();

        Throwable thrown = catchThrowable(() -> deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    formatter: foo"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("resource is pinned: log-handler:deployed:periodic-rotating-file:FOO:");
    }
}
