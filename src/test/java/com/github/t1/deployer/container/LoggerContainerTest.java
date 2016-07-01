package com.github.t1.deployer.container;

import com.github.t1.deployer.repository.Repository;
import com.github.t1.log.LogLevel;
import lombok.SneakyThrows;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.*;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LoggerContainerTest {
    private static final LogLevel ROOT_LEVEL = DEBUG;
    private static final LogLevel FOO_LEVEL = WARN;
    private static final LogLevel BAR_LEVEL = INFO;

    @InjectMocks
    LoggerContainer container;
    @Mock
    ModelControllerClient client;
    @Mock
    Repository repository;

    private Map<String, LogLevel> loggers = new LinkedHashMap<>();

    @Before
    @SneakyThrows(IOException.class)
    public void setupRootLogger() {
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
        when(client.execute(eq(readLoggersCli("*")), any(OperationMessageHandler.class)))
                .then(invocation -> ModelNode.fromString(successCli(readLoggersCliResult(this.loggers))));
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

    @SneakyThrows(IOException.class)
    private void givenLogger(String category, LogLevel level) {
        this.loggers.put(category, level);
        when(client.execute(eq(readLoggersCli(category)), any(OperationMessageHandler.class)))
                .thenReturn(ModelNode.fromString(successCli("{" + logger(level) + "}")));
    }

    private static ModelNode readLoggersCli(String loggerName) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("logger", loggerName);
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private String readLoggersCliResult(Map<String, LogLevel> loggers) {
        StringBuilder out = new StringBuilder();
        out.append("[");
        for (Map.Entry<String, LogLevel> logger : loggers.entrySet()) {
            if (out.length() > 1)
                out.append(", ");
            out.append("{")
               .append("\"address\" => [")
               .append("(\"subsystem\" => \"logging\"),")
               .append("(\"logger\" => \"").append(logger.getKey()).append("\")")
               .append("],");
            out.append("\"outcome\" => \"success\","
                    + "\"result\" => {").append(logger(logger.getValue())).append("}\n");
            out.append("}");
        }
        out.append("]");
        return out.toString();
    }

    private ModelNode verifyExecute(ModelNode node) throws IOException {
        return verify(client).execute(eq(node), any(OperationMessageHandler.class));
    }

    private String logger(LogLevel level) {
        return ""
                + "\"category\" => undefined," // deprecated: \"" + logger.getCategory() + "\","
                + "\"filter\" => undefined,"
                + "\"filter-spec\" => undefined,"
                + "\"handlers\" => undefined,"
                + "\"level\" => \"" + level + "\","
                + "\"use-parent-handlers\" => true" + "\n";
    }

    private ModelNode addLogger(String categoryType, String category, LogLevel logLevel, CharSequence... handlers) {
        return ModelNode.fromString("{"
                + loggerAddress(categoryType, category)
                + "\"operation\" => \"add\""
                + ((logLevel == null) ? "" : ",\"level\" => \"" + logLevel + "\"")
                + ((handlers.length == 0) ? "" : ",\"handlers\" => [\"" + String.join("\", \"", handlers) + "\"]\n")
                + "}");
    }

    private ModelNode writeLoggerAttribute(String categoryType, String category, LogLevel logLevel) {
        return ModelNode.fromString("{"
                + loggerAddress(categoryType, category)
                + "\"operation\" => \"write-attribute\","
                + "\"name\" => \"level\","
                + "\"value\" => \"" + logLevel + "\"\n"
                + "}");
    }

    private ModelNode removeLogger(String categoryType, String category) {
        return ModelNode.fromString("{"
                + "\"operation\" => \"remove\"\n" + loggerAddress(categoryType, category)
                + "}");
    }

    private String loggerAddress(String categoryType, String category) {
        return "\"address\" => [(\"subsystem\" => \"logging\"),(\"" + categoryType + "\" => \"" + category + "\")],";
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
                + "\"formatter\" => \"the-formatter\"\n"
                + "}");
    }

    public void assertIsRoot(LoggerResource logger) {
        assertThat(logger.isDeployed()).isTrue();
        assertThat(logger.isRoot()).isTrue();
        assertThat(logger.category()).isEqualTo("ROOT");
        assertThat(logger.level()).isEqualTo(DEBUG);
    }

    public void assertIsFoo(LoggerResource logger) {
        assertThat(logger.isDeployed()).isTrue();
        assertThat(logger.isRoot()).isFalse();
        assertThat(logger.category()).isEqualTo("foo");
        assertThat(logger.level()).isEqualTo(WARN);
    }

    public void assertIsBar(LoggerResource logger) {
        assertThat(logger.isDeployed()).isTrue();
        assertThat(logger.isRoot()).isFalse();
        assertThat(logger.category()).isEqualTo("bar");
        assertThat(logger.level()).isEqualTo(INFO);
    }

    @Test
    public void shouldGetJustRootLogger() {
        givenNoLogger("foo");
        givenNoLogger("bar");

        List<LoggerResource> loggers = container.allLoggers();

        assertThat(loggers).hasSize(1);
        assertIsRoot(loggers.get(0));
    }

    @Test
    public void shouldGetOneLogger() {
        givenLogger("foo", FOO_LEVEL);
        givenNoLogger("bar");

        List<LoggerResource> loggers = container.allLoggers();

        assertThat(loggers).hasSize(2);
        assertIsRoot(loggers.get(0));
        assertIsFoo(loggers.get(1));
    }

    @Test
    public void shouldGetTwoLoggersSorted() {
        givenLogger("foo", FOO_LEVEL);
        givenLogger("bar", BAR_LEVEL);

        List<LoggerResource> loggers = container.allLoggers();

        assertThat(loggers).hasSize(3);
        assertIsRoot(loggers.get(0));
        assertIsBar(loggers.get(1));
        assertIsFoo(loggers.get(2));
    }

    @Test
    public void shouldHaveOneLogger() {
        givenLogger("foo", FOO_LEVEL);
        givenNoLogger("bar");

        assertIsRoot(container.logger(""));

        assertIsFoo(container.logger("foo"));

        LoggerResource bar = container.logger("bar");
        assertThat(bar.isDeployed()).isFalse();
        assertThat(bar.isRoot()).isFalse();
        assertThat(bar.category()).isEqualTo("bar");
        assertThatThrownBy(bar::level).hasMessage("no logger 'bar'");
    }

    @Test
    public void shouldAddLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL);
        givenNoLogger("bar");

        container.logger("bar").toBuilder().level(INFO).build().add();

        verifyExecute(addLogger("logger", "bar", INFO));
    }

    @Test
    public void shouldAddLoggerWithOneHandler() throws IOException {
        givenNoLogger("bar");

        container.logger("bar").toBuilder().level(INFO).handler(new LogHandlerName("FOO")).build().add();

        verifyExecute(addLogger("logger", "bar", INFO, "FOO"));
    }

    @Test
    public void shouldAddLoggerWithTwoHandlers() throws IOException {
        givenNoLogger("bar");

        container.logger("bar").toBuilder()
                 .level(DEBUG)
                 .handler(new LogHandlerName("FOO"))
                 .handler(new LogHandlerName("BAR"))
                 .build()
                 .add();

        verifyExecute(addLogger("logger", "bar", DEBUG, "FOO", "BAR"));
    }

    @Test
    public void shouldAddLoggerWithOneHandlerAndNoLevel() throws IOException {
        givenNoLogger("bar");

        container.logger("bar").toBuilder()
                 .handler(new LogHandlerName("FOO"))
                 .build()
                 .add();

        verifyExecute(addLogger("logger", "bar", null, "FOO"));
    }

    @Test
    public void shouldFailToAddRootLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL);
        givenNoLogger("bar");

        assertThatThrownBy(() -> container.logger("").add())
                .hasMessage("can't add root logger");

        verify(client, never()).execute(eq(addLogger("root-logger", "ROOT", ERROR)),
                any(OperationMessageHandler.class));
    }

    @Test
    public void shouldUpdateLogLevel() throws IOException {
        givenLogger("foo", FOO_LEVEL);
        givenNoLogger("bar");

        container.logger("foo").correctLevel(ERROR);

        verifyExecute(writeLoggerAttribute("logger", "foo", ERROR));
    }

    @Test
    public void shouldUpdateRootLogLevel() throws IOException {
        givenLogger("foo", FOO_LEVEL);
        givenNoLogger("bar");

        container.logger("").correctLevel(ERROR);

        verifyExecute(writeLoggerAttribute("root-logger", "ROOT", ERROR));
    }

    @Test
    public void shouldRemoveLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL);
        givenNoLogger("bar");

        container.logger("foo").remove();

        verifyExecute(removeLogger("logger", "foo"));
    }

    @Test
    public void shouldFailToRemoveRootLogger() throws IOException {
        givenLogger("foo", FOO_LEVEL);
        givenNoLogger("bar");

        assertThatThrownBy(() -> container.logger("").remove())
                .hasMessage("can't remove root logger");

        verify(client, never()).execute(eq(removeLogger("root-logger", "ROOT")), any(OperationMessageHandler.class));
    }

    @Test
    public void shouldAddPeriodicRotatingFileLogHandler() throws Exception {
        LogHandler foo = container
                .handler(periodicRotatingFile, new LogHandlerName("FOO"))
                .toBuilder()
                .file("the-file")
                .suffix("the-suffix")
                .format("the-formatter")
                .build();

        foo.add();

        verifyExecute(addHandler("periodic-rotating-file-handler", "FOO"));
    }
}
