package com.github.t1.deployer.repository;

import static ch.qos.logback.classic.Level.*;
import io.dropwizard.*;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.lifecycle.ServerLifecycleListener;
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

import ch.qos.logback.classic.*;

import com.codahale.metrics.health.HealthCheck;

/**
 * If you don't have a real Artifactory Pro available, launch this, which will start a more or less working mock of
 * artifactory based on the local Maven repository in <code>~/.m2</code>. The checksums are read from an index file that
 * can be created with the {@link ArtifactoryMockIndexBuilder}.
 */
public class ArtifactoryMockLauncher extends Application<Configuration> {
    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            args = new String[] { "server" };
        new ArtifactoryMockLauncher().run(args);
    }

    private static class DummyHealthCheck extends HealthCheck {
        @Override
        protected Result check() {
            return Result.healthy();
        }
    }

    protected Server jettyServer;

    @Override
    public void run(Configuration configuration, Environment environment) {
        SimpleServerFactory serverConfig = new SimpleServerFactory();
        serverConfig.setApplicationContextPath("");
        configuration.setServerFactory(serverConfig);

        final HttpConnectorFactory connectorConfig = (HttpConnectorFactory) serverConfig.getConnector();
        connectorConfig.setPort(8081);

        environment.healthChecks().register("dummy", new DummyHealthCheck());

        environment.jersey().register(new ArtifactoryMock());

        setLogLevel("org.apache.http.wire", DEBUG);
        setLogLevel("com.github.t1.rest", DEBUG);
        setLogLevel("com.github.t1.deployer", DEBUG);

        environment.lifecycle().addServerLifecycleListener(new ServerLifecycleListener() {
            @Override
            public void serverStarted(Server server) {
                jettyServer = server;
                startConsole();
            }
        });
    }

    private void setLogLevel(String loggerName, Level level) {
        ((Logger) LoggerFactory.getLogger(loggerName)).setLevel(level);
    }

    private void startConsole() {
        // TODO report broken example to aesh
        new AeshConsoleBuilder() //
                .settings(new SettingsBuilder().ansi(false).create()) //
                .prompt(new Prompt("> ")) //
                .commandRegistry(new AeshCommandRegistryBuilder() //
                        .command(new ExitCommand()) //
                        .command(new IndexCommand()) //
                        .command(new HelpCommand()) //
                        .create()) //
                .create() //
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
            invocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "index", description = "rebuilds the file ~/.m2/checksum.index")
    public class IndexCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            new ArtifactoryMockIndexBuilder().run();
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
