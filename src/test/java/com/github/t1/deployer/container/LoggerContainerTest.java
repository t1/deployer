package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.log.LogLevel;
import lombok.SneakyThrows;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LoggerContainerTest {
    private static final LoggerConfig ROOT = LoggerConfig.builder().category("").level(DEBUG).build();
    private static final LoggerConfig FOO = LoggerConfig.builder().category("foo").level(WARN).build();
    private static final LoggerConfig BAR = LoggerConfig.builder().category("bar").level(INFO).build();

    @InjectMocks
    LoggerContainer container;
    @Mock
    ModelControllerClient client;
    @Mock
    Repository repository;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

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
                        + "        \"level\" => \"" + ROOT.getLevel() + "\"\n"
                        + "    }")));
    }

    private static ModelNode readRootLoggerCli() {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("root-logger", "ROOT");
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    @SneakyThrows(IOException.class)
    private void givenLoggers(LoggerConfig... loggers) {
        when(client.execute(eq(readLoggersCli("*")), any(OperationMessageHandler.class)))
                .thenReturn(ModelNode.fromString(successCli(readLoggersCliResult(loggers))));
        for (LoggerConfig logger : loggers) {
            when(client.execute(eq(readLoggersCli(logger.getCategory())), any(OperationMessageHandler.class)))
                    .thenReturn(ModelNode.fromString(successCli("{" + logger(logger) + "}")));
        }
    }

    @SneakyThrows(IOException.class)
    private void givenNoLogger(LoggerConfig logger) {
        when(client.execute(eq(readLoggersCli(logger.getCategory())), any(OperationMessageHandler.class)))
                .thenReturn(ModelNode.fromString("{\n"
                        + "    \"outcome\" => \"failed\",\n"
                        + "    \"failure-description\" => \"WFLYCTL0216: Management resource '[\n"
                        + "    (\\\"subsystem\\\" => \\\"logging\\\"),\n"
                        + "    (\\\"logger\\\" => \\\"" + logger.getCategory() + "\\\")\n"
                        + "]' not found\",\n"
                        + "    \"rolled-back\" => true\n"
                        + "}"));
    }

    private static ModelNode readLoggersCli(String loggerName) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("logger", loggerName);
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private String readLoggersCliResult(LoggerConfig... loggers) {
        StringBuilder out = new StringBuilder();
        out.append("[");
        for (LoggerConfig logger : loggers) {
            if (out.length() > 1)
                out.append(", ");
            out.append("{")
               .append("\"address\" => [")
               .append("(\"subsystem\" => \"logging\"),")
               .append("(\"logger\" => \"").append(logger.getCategory()).append("\")")
               .append("],");
            out.append("\"outcome\" => \"success\",\"result\" => {").append(logger(logger)).append("}\n");
            out.append("}");
        }
        out.append("]");
        return out.toString();
    }

    private String logger(LoggerConfig logger) {
        return ""
                + "\"category\" => undefined," // \"" + logger.getCategory() + "\","
                + "\"filter\" => undefined,"
                + "\"filter-spec\" => undefined,"
                + "\"handlers\" => undefined,"
                + "\"level\" => \"" + logger.getLevel() + "\","
                + "\"use-parent-handlers\" => true" + "\n";
    }

    private ModelNode addLogger(String categoryType, String category, LogLevel logLevel) {
        return ModelNode.fromString("{"
                + "\"address\" => [(\"subsystem\" => \"logging\"),(\"" + categoryType + "\" => \"" + category + "\")],"
                + "\"operation\" => \"add\",\"level\" => \"" + logLevel + "\"\n"
                + "}");
    }

    private ModelNode updateLogLevel(String categoryType, String category, LogLevel logLevel) {
        return ModelNode.fromString("{"
                + "\"address\" => [(\"subsystem\" => \"logging\"),(\"" + categoryType + "\" => \"" + category + "\")],"
                + "\"operation\" => \"write-attribute\",\"name\" => \"level\",\"value\" => \"" + logLevel + "\"\n"
                + "}");
    }

    private ModelNode removeLogger(String categoryType, String category) {
        return ModelNode.fromString("{"
                + "\"address\" => [(\"subsystem\" => \"logging\"),(\"" + categoryType + "\" => \"" + category + "\")],"
                + "\"operation\" => \"remove\"\n"
                + "}");
    }

    @Test
    public void shouldGetJustRootLogger() {
        givenLoggers();

        List<LoggerConfig> loggers = container.getLoggers();

        assertThat(loggers).containsExactly(ROOT);
    }

    @Test
    public void shouldGetOneLogger() {
        givenLoggers(FOO);

        List<LoggerConfig> loggers = container.getLoggers();

        assertThat(loggers).containsExactly(ROOT, FOO);
    }

    @Test
    public void shouldGetTwoLoggersSorted() {
        givenLoggers(FOO, BAR);

        List<LoggerConfig> loggers = container.getLoggers();

        assertThat(loggers).containsExactly(ROOT, BAR, FOO);
    }

    @Test
    public void shouldHaveOneLogger() {
        givenLoggers(FOO);
        givenNoLogger(BAR);

        softly.assertThat(container.hasLogger(ROOT.getCategory())).isTrue();
        softly.assertThat(container.hasLogger("")).isTrue();
        softly.assertThat(container.getLogger("")).isEqualTo(ROOT);

        softly.assertThat(container.hasLogger(FOO.getCategory())).isTrue();
        softly.assertThat(container.hasLogger("foo")).isTrue();
        softly.assertThat(container.getLogger("foo")).isEqualTo(FOO);

        softly.assertThat(container.hasLogger(BAR.getCategory())).isFalse();
        softly.assertThat(container.hasLogger("bar")).isFalse();
        softly.assertThatThrownBy(() -> container.getLogger("bar"))
              .hasMessage("no logger 'bar'");
    }

    @Test
    public void shouldAddLogger() throws IOException {
        givenLoggers(FOO);

        container.add(BAR);

        verify(client).execute(eq(addLogger("logger", "bar", INFO)), any(OperationMessageHandler.class));
    }

    @Test
    public void shouldFailToAddRootLogger() throws IOException {
        givenLoggers(FOO);

        assertThatThrownBy(() -> container.add(ROOT))
                .hasMessage("can't add root logger");

        verify(client, never()).execute(eq(addLogger("root-logger", "ROOT", ERROR)),
                any(OperationMessageHandler.class));
    }

    @Test
    public void shouldUpdateLogLevel() throws IOException {
        givenLoggers(FOO);

        container.setLogLevel(FOO.getCategory(), ERROR);

        verify(client).execute(eq(updateLogLevel("logger", "foo", ERROR)), any(OperationMessageHandler.class));
    }

    @Test
    public void shouldUpdateRootLogLevel() throws IOException {
        givenLoggers(FOO);

        container.setLogLevel("", ERROR);

        verify(client).execute(eq(updateLogLevel("root-logger", "ROOT", ERROR)), any(OperationMessageHandler.class));
    }

    @Test
    public void shouldRemoveLogger() throws IOException {
        givenLoggers(FOO);

        container.remove(FOO);

        verify(client).execute(eq(removeLogger("logger", "foo")), any(OperationMessageHandler.class));
    }

    @Test
    public void shouldFailToRemoveRootLogger() throws IOException {
        givenLoggers(FOO);

        assertThatThrownBy(() -> container.remove(ROOT))
                .hasMessage("can't remove root logger");

        verify(client, never()).execute(eq(removeLogger("root-logger", "ROOT")), any(OperationMessageHandler.class));
    }
}
