package com.github.t1.deployer.app;

import org.junit.Test;

import static com.github.t1.deployer.container.LogHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

public class LogHandlerDeployerTest extends AbstractDeployerTest {
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

        fixture.verifyWriteAttribute("formatter", "the-new-format");
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyWriteAttribute("formatter", null);
        fixture.expectChange("format", "the-format", null);
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    format: the-format\n");

        fixture.verifyWriteAttribute("formatter", "the-format");
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

        Audits audits = deploy(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    type: periodic-rotating-file\n"
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


    // TODO shouldRemoveHandlerWhenManaged

    @Test
    public void shouldAddCustomHandler() {
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
    public void shouldAddCustomHandlerProperty() {
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

        fixture.verifyMapPut("property", "bax", "bbb");
        fixture.expectChange("property/bax", null, "bbb").verifyChanged(audits);
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

        fixture.verifyMapPut("property", "foo", "bax");
        fixture.expectChange("property/foo", "bar", "bax").verifyChanged(audits);
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

        fixture.verifyMapRemove("property", "foos");
        fixture.expectChange("property/foos", "bars", null).verifyChanged(audits);
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

        fixture.verifyMapRemove("property", "foo");
        fixture.verifyMapPut("property", "bax", "bar");
        fixture.expectChange("property/bax", null, "bar")
               .expectChange("property/foo", "bar", null)
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

    // TODO shouldAddLoggerAndHandler
}
