package com.github.t1.deployer.repository;

import ch.qos.logback.classic.*;
import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.*;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.server.Server;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.*;
import org.jboss.aesh.console.command.*;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.*;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.slf4j.LoggerFactory;

import java.util.List;

import static ch.qos.logback.classic.Level.*;
import static java.util.Arrays.*;

/**
 * If you don't have a real Artifactory Pro available, launch this, which will start a more or less working mock of
 * artifactory based on the local Maven repository in <code>~/.m2</code>. The checksums are read from an index file that
 * can be created with the {@link ArtifactoryMockIndexBuilder}.
 */
public class ArtifactoryMockLauncher extends Application<Configuration> {
    public static void main(String... args) throws Exception {
        ArtifactoryMockLauncher launcher = new ArtifactoryMockLauncher(args);
        launcher.run("server");
    }

    private static class DummyHealthCheck extends HealthCheck {
        @Override
        protected Result check() {
            return Result.healthy();
        }
    }

    private final List<String> commands;

    private Server jettyServer;

    public ArtifactoryMockLauncher(String... args) { this.commands = asList(args); }

    @Override
    public void run(Configuration configuration, Environment environment) {
        SimpleServerFactory serverConfig = new SimpleServerFactory();
        serverConfig.setApplicationContextPath("/artifactory");
        configuration.setServerFactory(serverConfig);

        final HttpConnectorFactory connectorConfig = (HttpConnectorFactory) serverConfig.getConnector();
        connectorConfig.setPort(8081);

        environment.healthChecks().register("dummy", new DummyHealthCheck());

        environment.jersey().register(new ArtifactoryMock());

        setLogLevel("org.apache.http.wire", DEBUG);
        setLogLevel("com.github.t1.rest", DEBUG);
        setLogLevel("com.github.t1.deployer", DEBUG);

        if (commands.contains("cli"))
            environment.lifecycle().addServerLifecycleListener(server -> {
                jettyServer = server;
                startConsole();
            });
        if (commands.contains("index"))
            environment.lifecycle().addServerLifecycleListener(server -> {
                jettyServer = server;
                rebuildIndex();
            });
    }

    private void setLogLevel(String loggerName, Level level) {
        ((Logger) LoggerFactory.getLogger(loggerName)).setLevel(level);
    }

    private void rebuildIndex() {
        new ArtifactoryMockIndexBuilder().run();
    }

    private void startConsole() {
        // TODO report broken example to aesh
        new AeshConsoleBuilder()
                .settings(new SettingsBuilder().ansi(false).create())
                .prompt(new Prompt("> "))
                .commandRegistry(new AeshCommandRegistryBuilder()
                        .command(new ExitCommand())
                        .command(new IndexCommand())
                        .command(new HelpCommand())
                        .create())
                .create()
                .start();
    }

    @CommandDefinition(name = "exit", description = "quits the service and the console")
    public class ExitCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            try {
                jettyServer.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (invocation != null)
                invocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "index", description = "rebuilds the file ~/.m2/checksum.index")
    public class IndexCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            rebuildIndex();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "help", description = "shows all available commands")
    public class HelpCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            CommandRegistry registry = invocation.getCommandRegistry();
            for (String commandName : registry.getAllCommandNames()) {
                System.out.print(commandName + ": " + invocation.getHelpInfo(commandName).replace("Usage: ", ""));
            }
            return CommandResult.SUCCESS;
        }
    }
}
