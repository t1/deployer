package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixture.VersionFixture;
import com.github.t1.deployer.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

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

        verify(deploymentContainer).deploy(new DeploymentName("bar"), foo.inputStream());
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

        verify(loggerContainer).add(new LoggerConfig("com.github.t1.deployer.app", DEBUG));
    }


    // TODO shouldUpdateLogLevel
    // TODO shouldAddHandler
    // TODO shouldRemoveLoggerWhenStateIsUndeployed
    // TODO shouldRemoveLoggerWhenManaged
    // TODO shouldRemoveHandlerWhenStateIsUndeployed
    // TODO shouldRemoveHandlerWhenManaged


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
