package com.github.t1.deployer.app;

import com.github.t1.deployer.app.NewLoggerContainerTest.LoggerFixture.LoggerFixtureBuilder;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.log.LogLevel;
import lombok.SneakyThrows;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import javax.enterprise.inject.Instance;
import javax.validation.constraints.NotNull;
import java.io.IOException;

import static com.github.t1.deployer.testtools.TestData.*;
import static com.github.t1.log.LogLevel.*;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

/** TODO this style would be better */
@RunWith(MockitoJUnitRunner.class)
public class NewLoggerContainerTest {
    @InjectMocks Deployer deployer;

    @Mock ArtifactContainer artifacts;
    @Mock Repository repository;
    @Spy Audits audits;
    @Mock Instance<Audits> auditsInstance;

    @InjectMocks LoggerContainer loggers;

    @Mock ModelControllerClient client;

    @Before
    public void setUp() throws Exception {
        deployer.loggers = loggers;

        when(artifacts.getAllArtifacts()).thenReturn(emptyList());
    }


    public LoggerFixtureBuilder givenLogger(String category) {
        return LoggerFixture.builder().client(client).category(category);
    }

    @lombok.Value
    @lombok.Builder
    public static class LoggerFixture {
        @NotNull String category;
        LogLevel level;
        Boolean deployed;

        @NotNull ModelControllerClient client;

        public static class LoggerFixtureBuilder {
            private LoggerFixtureBuilder deployed(Boolean deployed) {
                this.deployed = deployed;
                return this;
            }

            public LoggerFixture deployed() { return deployed(true).build().mock(); }

            public LoggerFixture undeployed() { return deployed(false).build().mock(); }

        }

        @SneakyThrows(IOException.class)
        private LoggerFixture mock() {
            when(client.execute(eq(readLoggersCli(this.category)), any(OperationMessageHandler.class))).then(i -> this);
            return this;
        }

        public void verifyAdded(Audits audits) {

        }

        @SneakyThrows(IOException.class)
        private void givenLogger(String category, LogLevel level, boolean useParentHandlers) {
            // this.loggers.put(category, level);
            when(client.execute(eq(readLoggersCli(category)), any(OperationMessageHandler.class)))
                    .thenReturn(ModelNode.fromString(successCli("{" + logger(level, useParentHandlers) + "}")));
        }

        private static ModelNode readLoggersCli(String loggerName) {
            ModelNode node = new ModelNode();
            node.get("address").add("subsystem", "logging").add("logger", loggerName);
            node.get("operation").set("read-resource");
            node.get("recursive").set(true);
            return node;
        }

        private static String logger(LogLevel level, boolean useParentHandlers) {
            return ""
                    + "\"category\" => undefined," // deprecated: \"" + logger.getCategory() + "\","
                    + "\"filter\" => undefined,"
                    + "\"filter-spec\" => undefined,"
                    + "\"handlers\" => undefined,"
                    + "\"level\" => \"" + level + "\","
                    + "\"use-parent-handlers\" => " + useParentHandlers + "\n";
        }
    }


    @Test
    public void shouldAddEmptyLoggers() {
        deployer.run(""
                + "loggers:\n");
    }


    @Test
    public void shouldFailToAddLoggerWithoutItem() {
        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "loggers:\n"
                + "  foo:\n"));

        assertThat(thrown).hasMessageContaining("exception while loading config plan");
        assertThat(thrown.getCause()).hasMessageContaining("no config in logger 'foo'");
    }


    @Test
    @Ignore
    public void shouldAddLogger() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG).undeployed();

        Audits audits = deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n");

        fixture.verifyAdded(audits);
    }


}
