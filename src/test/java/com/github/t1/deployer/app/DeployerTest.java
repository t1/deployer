package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixture.VersionFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.StringReader;

@RunWith(MockitoJUnitRunner.class)
public class DeployerTest extends AbstractDeployerTest {
    @InjectMocks Deployer deployer;

    @Test
    public void shouldDeployWebArchive() {
        VersionFixture foo = givenArtifact("foo").version("1.3.2");
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1.3.2\n"));

        deployer.run(plan);

        foo.verifyDeployed();
    }


    @Test
    public void shouldNotRedeployWebArchiveWithSameNameAndChecksum() {
        givenArtifact("foo")
                .version("1").deployed()
                .and()
                .version("2");
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: \"1\"\n"));

        deployer.run(plan);

        // #after(): no deploy operations
    }


    @Test
    public void shouldUpdateExistingWebArchive() throws Exception {
        VersionFixture foo2 = givenArtifact("foo")
                .version("1").deployed()
                .and()
                .version("2");
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: \"2\"\n"));

        deployer.run(plan);

        foo2.verifyRedeployed();
    }


    @Test
    public void shouldDeploySecondWebArchive() throws Exception {
        givenArtifact("jolokia")
                .version("1.3.2").deployed()
                .and()
                .version("1.3.3");
        VersionFixture mockserver = givenArtifact("org.mock-server", "mockserver-war")
                .version("3.10.4");
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"
                + "org.mock-server:\n"
                + "  mockserver-war:\n"
                + "    version: 3.10.4\n"));

        deployer.run(plan);

        // #after(): jolokia not undeployed
        mockserver.verifyDeployed();
    }


    @Test
    public void shouldUndeployWebArchiveWhenStateIsUndeployed() {
        VersionFixture foo = givenArtifact("foo").version("1.3.2").deployed();
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed\n"));

        deployer.run(plan);

        foo.verifyUndeployed();
    }


    @Test
    public void shouldUndeployUnspecifiedWebArchiveWhenManaged() {
        VersionFixture jolokia = givenArtifact("jolokia").version("1.3.2").deployed();
        VersionFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        deployer.setManaged(true);
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"));

        deployer.run(plan);

        // #after(): jolokia not undeployed
        mockserver.verifyUndeployed();
    }


    // TODO shouldDeployJdbcDriver
    // TODO shouldDeployBundle
    // TODO shouldDeployTemplate

    // @Test
    // public void shouldUndeployEverything() throws Exception {
    //     // TODO manage configs
    //     ConfigurationPlan plan = ConfigurationPlan.load(new StringReader("---\n"));
    //
    //     deployer.run(plan);
    //
    //     assertDeployments();
    //     // TODO assertThat(jbossConfig.read()).isEqualTo(jbossConfig.getOrig());
    // }
}
