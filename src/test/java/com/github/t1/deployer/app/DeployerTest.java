package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixture.VersionFixture;
import com.github.t1.deployer.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployerTest extends AbstractDeployerTest {
    @InjectMocks Deployer deployer;

    @Test
    public void shouldDeployWebArchive() {
        VersionFixture foo = givenArtifact("foo").version("1.3.2");

        deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test
    public void shouldNotRedeployWebArchiveWithSameNameAndChecksum() {
        givenArtifact("foo")
                .version("1").deployed()
                .and()
                .version("2");

        deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: \"1\"\n");

        // #after(): no deploy operations
    }


    @Test
    public void shouldDeployWebArchiveWithOtherName() {
        VersionFixture foo = givenArtifact("foo").version("1.3.2");

        deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1.3.2\n"
                + "    name: bar");

        verify(deployments).deploy(new DeploymentName("bar"), foo.inputStream());
    }


    @Test
    public void shouldUpdateExistingWebArchive() {
        VersionFixture foo2 = givenArtifact("foo")
                .version("1").deployed()
                .and()
                .version("2");

        deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: \"2\"\n");

        foo2.verifyRedeployed();
    }


    @Test
    public void shouldDeploySecondWebArchive() {
        givenArtifact("jolokia")
                .version("1.3.2").deployed()
                .and()
                .version("1.3.3");
        VersionFixture mockserver = givenArtifact("org.mock-server", "mockserver-war")
                .version("3.10.4");

        deployer.run(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"
                + "org.mock-server:\n"
                + "  mockserver-war:\n"
                + "    version: 3.10.4\n");

        // #after(): jolokia not undeployed
        mockserver.verifyDeployed();
    }


    @Test
    public void shouldUndeployWebArchiveWhenStateIsUndeployed() {
        VersionFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed\n");

        foo.verifyUndeployed();
    }


    @Test
    public void shouldUndeployUnspecifiedWebArchiveWhenManaged() {
        givenArtifact("jolokia").version("1.3.2").deployed();
        VersionFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        deployer.setManaged(true);

        deployer.run(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n");

        // #after(): jolokia not undeployed
        mockserver.verifyUndeployed();
    }


    @Test
    public void shouldAddLogger() {
        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n");

        verify(loggers).add(new LoggerConfig("com.github.t1.deployer.app", DEBUG));
    }


    @Test
    public void shouldNotAddExistingLogger() {
        givenLogger("com.github.t1.deployer.app").level(DEBUG).exists();

        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n");

        // #after(): no add nor update
    }


    @Test
    public void shouldUpdateLogLevel() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG).exists();

        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: INFO\n");

        verify(loggers).setLogLevel(fixture.getCategory(), INFO);
    }


    @Test
    public void shouldRemoveExistingLoggerWhenStateIsUndeployed() {
        LoggerFixture fixture = givenLogger("com.github.t1.deployer.app").level(DEBUG).exists();

        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: undeployed\n");

        verify(loggers).remove(new LoggerConfig(fixture.getCategory(), ALL));
    }


    @Test
    public void shouldRemoveNonExistingLoggerWhenStateIsUndeployed() {
        givenLogger("com.github.t1.deployer.app").level(DEBUG);

        deployer.run(""
                + "loggers:\n"
                + "  com.github.t1.deployer.app:\n"
                + "    level: DEBUG\n"
                + "    state: undeployed\n");

        // #after(): not undeployed
    }


    // TODO shouldRemoveLoggerWhenManaged


    @Test
    public void shouldAddPeriodicRotatingFileHandlerAsDefault() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter");

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyAdded();
    }


    @Test
    public void shouldAddPeriodicRotatingFileHandler() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter");

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyAdded();
    }


    @Test
    public void shouldNotAddExistingHandler() {
        givenLogHandler(periodicRotatingFile, "FOO")
                .level(ALL)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .deployed();

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        // #after(): not added/updated
    }


    @Test
    public void shouldUpdateHandlerLevel() {
        LogHandlerFixture fixture = givenLogHandler(periodicRotatingFile, "FOO")
                .level(DEBUG)
                .file("the-file")
                .suffix("the-suffix")
                .formatter("the-formatter")
                .deployed();

        deployer.run(""
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    handler-type: periodicRotatingFile\n"
                + "    level: ALL\n"
                + "    file: the-file\n"
                + "    suffix: the-suffix\n"
                + "    formatter: the-formatter\n");

        fixture.verifyUpdated().level(ALL);
    }


    // TODO shouldUpdateHandlerLevel
    // TODO shouldUpdateHandlerFile
    // TODO shouldUpdateHandlerSuffix
    // TODO shouldUpdateHandlerFormatter
    // TODO shouldUpdateHandlerFileAndSuffix
    // TODO shouldRemoveHandlerWhenStateIsUndeployed
    // TODO shouldRemoveHandlerWhenManaged
    // TODO shouldAddConsoleHandler
    // TODO shouldAddCustomHandler


    // TODO shouldDeployBundle
    // TODO shouldDeployBundleWithParams
    // TODO shouldDeployDataSource
    // TODO shouldDeployXADataSource


    @Test
    public void shouldUndeployEverythingWhenManagedAndEmptyPlan() {
        VersionFixture jolokia = givenArtifact("jolokia").version("1.3.2").deployed();
        VersionFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        deployer.setManaged(true);

        deployer.run("---\n");

        jolokia.verifyUndeployed();
        mockserver.verifyUndeployed();
    }
}
