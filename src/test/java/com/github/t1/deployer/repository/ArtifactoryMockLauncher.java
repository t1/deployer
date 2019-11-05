package com.github.t1.deployer.repository;

import com.github.t1.jaxrsclienttest.JaxRsTestExtension;
import com.github.t1.jaxrsclienttest.JaxRsTestExtension.DummyApp;
import lombok.extern.slf4j.Slf4j;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.SettingsBuilder;

import javax.ws.rs.ApplicationPath;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * If you don't have a real Artifactory Pro available, launch this, which will start a more or less working mock of
 * artifactory based on the local Maven repository in <code>~/.m2</code>. The checksums are read from an index file that
 * can be created with the {@link ArtifactoryMockIndexBuilder}.
 */
@Slf4j
public class ArtifactoryMockLauncher {

    public static void main(String... args) {
        ArtifactoryMockLauncher launcher = new ArtifactoryMockLauncher(args);
        launcher.run();
    }

    private final List<String> commands;

    private JaxRsTestExtension container = new JaxRsTestExtension(
        new ArtifactoryMock(),
        new ProblemDetailMessageBodyWriter()
    )
        .contextPath("/artifactory")
        .port(8081);

    public ArtifactoryMockLauncher(String... args) { this.commands = asList(args); }

    private void run() {
        container.start();

        log.debug("started at {}", container.baseUri());

        if (commands.contains("cli"))
            startConsole();
        if (commands.contains("index"))
            rebuildIndex();
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

    @ApplicationPath("/artifactory") public static class ArtifactoryApp extends DummyApp {}

    @CommandDefinition(name = "exit", description = "quits the service and the console")
    public class ExitCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            try {
                container.stop();
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
    public static class HelpCommand implements Command<CommandInvocation> {
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
