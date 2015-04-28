package com.github.t1.deployer.container;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.log.LogLevel.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import lombok.SneakyThrows;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.deployer.repository.Repository;

@RunWith(MockitoJUnitRunner.class)
public class ContainerLoggersTest {
    @InjectMocks
    LoggerContainer container;
    @Mock
    ModelControllerClient client;
    @Mock
    Repository repository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @SneakyThrows(IOException.class)
    private void givenLoggers(String... loggers) {
        when(client.execute(eq(readAllLoggersCli()), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(successCli(readLoggersCliResult(loggers))));
    }

    private static ModelNode readAllLoggersCli() {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("logger", "*");
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private String readLoggersCliResult(String... loggers) {
        StringBuilder out = new StringBuilder();
        out.append("[");
        for (String logger : loggers) {
            if (out.length() > 1)
                out.append(", ");
            out.append("{") //
                    .append("\"address\" => [") //
                    .append("(\"subsystem\" => \"logging\"),") //
                    .append("(\"logger\" => \"").append(logger).append("\")") //
                    .append("],") //
                    .append("\"outcome\" => \"success\",") //
                    .append("\"result\" => {") //
                    .append("\"category\" => \"").append(logger).append("\",") //
                    .append("\"filter\" => undefined,") //
                    .append("\"filter-spec\" => undefined,") //
                    .append("\"handlers\" => undefined,") //
                    .append("\"level\" => \"WARN\",") //
                    .append("\"use-parent-handlers\" => true") //
                    .append("}") //
                    .append("}");
        }
        out.append("]");
        return out.toString();
    }

    @Test
    public void shouldGetNoLogger() {
        givenLoggers();

        List<LoggerConfig> loggers = container.getLoggers();

        assertEquals(emptyList(), loggers);
    }

    @Test
    public void shouldGetOneLogger() {
        givenLoggers("foo");

        List<LoggerConfig> loggers = container.getLoggers();

        assertEquals(1, loggers.size());
        assertEquals("foo", loggers.get(0).getCategory());
        assertEquals(WARN, loggers.get(0).getLevel());
    }

    @Test
    public void shouldGetTwoLoggers() {
        givenLoggers("foo", "bar");

        List<LoggerConfig> loggers = container.getLoggers();

        assertEquals(2, loggers.size());
        assertEquals("foo", loggers.get(0).getCategory());
        assertEquals(WARN, loggers.get(0).getLevel());
        assertEquals("bar", loggers.get(1).getCategory());
        assertEquals(WARN, loggers.get(1).getLevel());
    }
}
