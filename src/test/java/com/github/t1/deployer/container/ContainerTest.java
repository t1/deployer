package com.github.t1.deployer.container;

import com.github.t1.log.LogLevel;
import lombok.SneakyThrows;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static com.github.t1.deployer.container.LogHandlerType.*;
import static com.github.t1.deployer.container.LoggerCategory.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContainerTest {
    private static final LogLevel ROOT_LEVEL = DEBUG;
    private static final LogLevel FOO_LEVEL = WARN;

    private static final LogHandlerName FILE = new LogHandlerName("FILE");
    private static final LoggerCategory BAR = LoggerCategory.of("bar");
    private static final LoggerCategory FOO = LoggerCategory.of("foo");

    Container container = new Container();
    @Mock
    ModelControllerClient client;

    @Before
    @SneakyThrows(IOException.class)
    public void setup() {
        container.cli = new CLI();
        container.cli.client = client;

        when(client.execute(any(ModelNode.class), any(OperationMessageHandler.class)))
                .thenReturn(ModelNode.fromString("{\"outcome\" => \"success\"}"));
        when(client.execute(eq(readRootLoggerCli()), any(OperationMessageHandler.class)))
                .thenReturn(ModelNode.fromString(successCli("{\n"
                        + "        \"filter\" => undefined,\n"
                        + "        \"filter-spec\" => undefined,\n"
                        + "        \"handlers\" => [\n"
                        + "            \"CONSOLE\",\n"
                        + "            \"FILE\"\n"
                        + "        ],\n"
                        + "        \"level\" => \"" + ROOT_LEVEL + "\"\n"
                        + "    }")));
    }

    private static String successCli(String result) {
        return "{\n"
                + "\"outcome\" => \"success\",\n"
                + "\"result\" => " + result + "\n"
                + "}\n";
    }

    @After
    public void tearDown() throws Exception {
        verify(client, atLeast(0)).execute(eq(readRootLoggerCli()), any(OperationMessageHandler.class));
        verifyLoggerRead("*");
        verifyNoMoreInteractions(client);
    }

    private static ModelNode readRootLoggerCli() {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("root-logger", "ROOT");
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    @SneakyThrows(IOException.class)
    private void givenNoLogger(String category) {
        when(client.execute(eq(readLoggersCli(category)), any(OperationMessageHandler.class)))
                .thenReturn(ModelNode.fromString("{\n"
                        + "    \"outcome\" => \"failed\",\n"
                        + "    \"failure-description\" => \"WFLYCTL0216: Management resource '[\n"
                        + "    (\\\"subsystem\\\" => \\\"logging\\\"),\n"
                        + "    (\\\"logger\\\" => \\\"" + category + "\\\")\n"
                        + "]' not found\",\n"
                        + "    \"rolled-back\" => true\n"
                        + "}"));
    }

    private void givenLogger(String category, LogLevel level, boolean useParentHandlers, String... handlers) {
        givenLogger(category, level.name(), useParentHandlers, handlers);
    }

    @SneakyThrows(IOException.class)
    private void givenLogger(String category, String level, boolean useParentHandlers, String... handlers) {
        when(client.execute(eq(readLoggersCli(category)), any(OperationMessageHandler.class)))
                .thenReturn(ModelNode.fromString(successCli("{" + logger(level, useParentHandlers, handlers) + "}")));
    }

    private static ModelNode readLoggersCli(String category) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("logger", category);
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private ModelNode verifyExecute(ModelNode node) throws IOException {
        return verify(client).execute(eq(node), any(OperationMessageHandler.class));
    }

    private String logger(String level, boolean useParentHandlers, String... handlers) {
        return ""
                + "\"category\" => undefined," // deprecated: \"" + logger.getCategory() + "\","
                + "\"filter\" => undefined,"
                + "\"filter-spec\" => undefined,"
                + "\"handlers\" => " + toCliList(handlers) + ","
                + "\"level\" => \"" + level + "\","
                + "\"use-parent-handlers\" => " + useParentHandlers + "\n";
    }

    private ModelNode addLogger(String categoryType, String category, LogLevel logLevel, Boolean useParentHandlers,
            String... handlers) {
        return ModelNode.fromString("{"
                + loggerAddress(categoryType, category)
                + ",\"operation\" => \"add\"\n"
                + ((logLevel == null) ? "" : ",\"level\" => \"" + logLevel + "\"\n")
                + ((handlers.length == 0) ? "" : ",\"handlers\" => " + toCliList(handlers) + "\n")
                + ((useParentHandlers == null) ? "" : ",\"use-parent-handlers\" => " + useParentHandlers + "\n")
                + "}");
    }

    private String toCliList(String... handlers) {
        return (handlers.length == 0) ? "undefined" : "[\"" + String.join("\",\"", (CharSequence[]) handlers) + "\"]";
    }

    private ModelNode writeLoggingAttribute(String categoryType, String category, String name, Object value) {
        if (value instanceof String || value instanceof Enum)
            value = "\"" + value + "\"";
        return ModelNode.fromString("{"
                + loggerAddress(categoryType, category)
                + ",\"operation\" => \"write-attribute\","
                + "\"name\" => \"" + name + "\","
                + "\"value\" => " + value + "\n"
                + "}");
    }

    private ModelNode removeLogger(String categoryType, String category) {
        return ModelNode.fromString("{"
                + loggerAddress(categoryType, category)
                + ",\"operation\" => \"remove\"\n"
                + "}");
    }

    private String loggerAddress(String categoryType, String category) {
        return "\"address\" => [(\"subsystem\" => \"logging\"),(\"" + categoryType + "\" => \"" + category + "\")]\n";
    }

    private ModelNode addHandler(String type, String name) {
        return ModelNode.fromString("{"
                + "\"address\" => [(\"subsystem\" => \"logging\"),(\"" + type + "\" => \"" + name + "\")],"
                + "\"operation\" => \"add\","
                + "\"file\" => {\n"
                + "  \"path\" => \"the-file\",\n"
                + "  \"relative-to\" => \"jboss.server.log.dir\"\n"
                + "},\n"
                + "\"suffix\" => \"the-suffix\",\n"
                + "\"named-formatter\" => \"the-formatter\"\n"
                + "}");
    }

    public void verifyLoggerRead(String loggerName) throws IOException {
        verify(client, atLeast(0)).execute(eq(readLoggersCli(loggerName)), any(OperationMessageHandler.class));
    }

    public void assertIsRoot(LoggerResource logger) {
        assertThat(logger.isDeployed()).isTrue();
        assertThat(logger.isRoot()).isTrue();
        assertThat(logger.category()).isEqualTo(ROOT);
        assertThat(logger.level()).isEqualTo(DEBUG);
    }

    public void assertIsFoo(LoggerResource logger) {
        assertThat(logger.isDeployed()).isTrue();
        assertThat(logger.isRoot()).isFalse();
        assertThat(logger.category()).isEqualTo(FOO);
        assertThat(logger.level()).isEqualTo(WARN);
    }

    public void assertIsBar(LoggerResource logger) {
        assertThat(logger.isDeployed()).isTrue();
        assertThat(logger.isRoot()).isFalse();
        assertThat(logger.category()).isEqualTo(BAR);
        assertThat(logger.level()).isEqualTo(INFO);
    }

    @Test
    public void shouldHaveOneLogger() throws Exception {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        assertIsRoot(container.logger(ROOT).build());

        assertIsFoo(container.logger(LoggerCategory.of("foo")).build());

        LoggerResource bar = container.logger(BAR).build();
        assertThat(bar.isDeployed()).isFalse();
        assertThat(bar.isRoot()).isFalse();
        assertThat(bar.category()).isEqualTo(BAR);
        assertThatThrownBy(bar::level).hasMessage("not deployed 'Logger:bar:deployed=false:null:[]'");
        verifyLoggerRead("foo");
        verifyLoggerRead("bar");
    }


    @Test
    public void shouldReadLoggerWithLevelOff() throws Exception {
        shouldReadLoggerWithLevel("OFF", OFF);
    }

    @Test
    public void shouldReadLoggerWithLevelTrace() throws Exception {
        shouldReadLoggerWithLevel("TRACE", TRACE);
    }

    @Test
    public void shouldReadLoggerWithLevelFinest() throws Exception {
        shouldReadLoggerWithLevel("FINEST", TRACE);
    }

    @Test
    public void shouldReadLoggerWithLevelFiner() throws Exception {
        shouldReadLoggerWithLevel("FINER", DEBUG);
    }

    @Test
    public void shouldReadLoggerWithLevelFine() throws Exception {
        shouldReadLoggerWithLevel("FINE", DEBUG);
    }

    @Test
    public void shouldReadLoggerWithLevelDebug() throws Exception {
        shouldReadLoggerWithLevel("DEBUG", DEBUG);
    }

    @Test
    public void shouldReadLoggerWithLevelConfig() throws Exception {
        shouldReadLoggerWithLevel("CONFIG", INFO);
    }

    @Test
    public void shouldReadLoggerWithLevelInfo() throws Exception {
        shouldReadLoggerWithLevel("INFO", INFO);
    }

    @Test
    public void shouldReadLoggerWithLevelWarn() throws Exception {
        shouldReadLoggerWithLevel("WARN", WARN);
    }

    @Test
    public void shouldReadLoggerWithLevelWarning() throws Exception {
        shouldReadLoggerWithLevel("WARNING", WARN);
    }

    @Test
    public void shouldReadLoggerWithLevelSevere() throws Exception {
        shouldReadLoggerWithLevel("SEVERE", ERROR);
    }

    @Test
    public void shouldReadLoggerWithLevelFatal() throws Exception {
        shouldReadLoggerWithLevel("FATAL", ERROR);
    }

    @Test
    public void shouldReadLoggerWithLevelError() throws Exception {
        shouldReadLoggerWithLevel("ERROR", ERROR);
    }

    @Test
    public void shouldReadLoggerWithLevelAll() throws Exception {
        shouldReadLoggerWithLevel("ALL", LogLevel.ALL);
    }

    public void shouldReadLoggerWithLevel(String from, LogLevel to) throws IOException {
        givenLogger("foo", from, true);

        LoggerResource foo = container.logger(FOO).build();

        assertThat(foo.level()).isEqualTo(to);
        verifyLoggerRead("foo");
    }


    @Test
    public void shouldAddLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        container.logger(BAR).level(INFO).build().add();

        verifyExecute(addLogger("logger", "bar", INFO, null));
    }

    @Test
    public void shouldAddLoggerWithUseParentHandlersTrue() throws IOException {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        container.logger(BAR).level(INFO).useParentHandlers(true).build().add();

        verifyExecute(addLogger("logger", "bar", INFO, true));
    }

    @Test
    public void shouldAddLoggerWithUseParentHandlersFalse() throws IOException {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        container.logger(BAR).level(INFO).useParentHandlers(false).build().add();

        verifyExecute(addLogger("logger", "bar", INFO, false));
    }

    @Test
    public void shouldAddLoggerWithOneHandler() throws IOException {
        givenNoLogger("bar");

        container.logger(BAR).level(INFO).handler(new LogHandlerName("FOO")).build().add();

        verifyExecute(addLogger("logger", "bar", INFO, null, "FOO"));
    }

    @Test
    public void shouldAddLoggerWithTwoHandlers() throws IOException {
        givenNoLogger("bar");

        container.logger(BAR)
                 .level(DEBUG)
                 .handler(new LogHandlerName("FOO"))
                 .handler(new LogHandlerName("BAR"))
                 .build()
                 .add();

        verifyExecute(addLogger("logger", "bar", DEBUG, null, "FOO", "BAR"));
    }

    @Test
    public void shouldAddLoggerWithOneHandlerAndNoLevel() throws IOException {
        givenNoLogger("bar");

        container.logger(BAR)
                 .handler(new LogHandlerName("FOO"))
                 .build()
                 .add();

        verifyExecute(addLogger("logger", "bar", null, null, "FOO"));
    }

    @Test
    public void shouldFailToAddRootLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        assertThatThrownBy(() -> container.logger(ROOT).build().add())
                .hasMessage("can't add root logger");

        verify(client, never()).execute(eq(addLogger("root-logger", "ROOT", ERROR, null)),
                any(OperationMessageHandler.class));
    }

    @Test
    public void shouldUpdateLogLevel() throws IOException {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        container.logger(FOO).build().writeLevel(ERROR);

        verifyExecute(writeLoggingAttribute("logger", "foo", "level", ERROR));
        verifyLoggerRead("foo");
    }

    @Test
    public void shouldUpdateRootLogLevel() throws IOException {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        container.logger(ROOT).build().writeLevel(ERROR);

        verifyExecute(writeLoggingAttribute("root-logger", "ROOT", "level", ERROR));
    }

    @Test
    public void shouldUpdateUseParentHandlerToTrue() throws IOException {
        givenLogger("foo", FOO_LEVEL, false);
        givenNoLogger("bar");

        container.logger(FOO).build().writeUseParentHandlers(true);

        verifyExecute(readLoggersCli("foo"));
        verifyExecute(writeLoggingAttribute("logger", "foo", "use-parent-handlers", true));
    }

    @Test
    public void shouldUpdateUseParentHandlerToFalse() throws IOException {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        container.logger(FOO).build().writeUseParentHandlers(false);

        verifyExecute(writeLoggingAttribute("logger", "foo", "use-parent-handlers", false));
        verifyLoggerRead("foo");
    }

    @Test
    public void shouldAddLogHandlerToLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL, false, "CONSOLE");

        container.logger(FOO).build().addLoggerHandler(FILE);

        verifyExecute(ModelNode.fromString("{"
                + loggerAddress("logger", "foo")
                + ",\"operation\" => \"add-handler\""
                + ",\"name\" => \"FILE\""
                + "}"));
        verifyLoggerRead("foo");
    }

    @Test
    public void shouldRemoveLogHandlerToLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL, false, "CONSOLE", "FILE");

        container.logger(FOO).build().removeLoggerHandler(FILE);

        verifyExecute(ModelNode.fromString("{"
                + loggerAddress("logger", "foo")
                + ",\"operation\" => \"remove-handler\""
                + ",\"name\" => \"FILE\""
                + "}"));
        verifyLoggerRead("foo");
    }

    @Test
    public void shouldRemoveLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        container.logger(FOO).build().remove();

        verifyExecute(removeLogger("logger", "foo"));
    }

    @Test
    public void shouldFailToRemoveRootLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL, true);
        givenNoLogger("bar");

        assertThatThrownBy(() -> container.logger(ROOT).build().remove())
                .hasMessage("can't remove root logger");

        verify(client, never()).execute(eq(removeLogger("root-logger", "ROOT")), any(OperationMessageHandler.class));
    }

    @Test
    public void shouldAddPeriodicRotatingFileLogHandler() throws Exception {
        LogHandlerResource foo = container
                .logHandler(periodicRotatingFile, new LogHandlerName("FOO"))
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .build();

        foo.add();

        verifyExecute(addHandler("periodic-rotating-file-handler", "FOO"));
    }
}
