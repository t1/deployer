package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixtureBuilder.ArtifactFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.t1.deployer.model.ArtifactType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactDeployerTest extends AbstractDeployerTest {
    @Test
    public void shouldDeployWebArchive() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
        );

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployEmptyArtifacts() {
        Audits audits = deployer.run(""
                + "artifacts:\n");

        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldFailToDeployWebArchiveWithEmptyItem() {
        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "artifacts:\n"
                + "  foo-war:\n"));

        assertThat(thrown.getCause()).hasMessageContaining("no config in artifact 'foo-war'");
    }


    @Test
    public void shouldFailToDeployWebArchiveWithoutVersion() {
        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "artifacts:\n"
                + "  foo-war:\n"
                + "    group-id: org.foo\n"
                + "    state: deployed\n"));

        assertThat(thrown.getCause()).hasMessageContaining("version");
    }


    @Test
    public void shouldDeployWebArchiveWithOtherName() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();
        ArtifactFixture bar = givenArtifact("bar", foo.groupId(), foo.artifactId()).version("1.3.2");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  bar:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo\n"
                + "    version: 1.3.2\n"
        );

        bar.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithVariables() {
        systemProperties.given("fooGroupId", "org.foo");
        systemProperties.given("fooArtifactId", "foo");
        systemProperties.given("fooVersion", "1.3.2");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  ${fooArtifactId}:\n"
                + "    group-id: ${fooGroupId}\n"
                + "    version: ${fooVersion}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithTwoVariablesInOneLine() {
        systemProperties.given("orgVar", "org");
        systemProperties.given("fooVar", "foo");
        ArtifactFixture foo = givenArtifact("foo").version("1");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: ${orgVar}.${fooVar}\n"
                + "    version: 1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithCommentAfterVariable() {
        systemProperties.given("orgVar", "org");
        ArtifactFixture foo = givenArtifact("foo").version("1");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: ${orgVar}.foo # cool\n"
                + "    version: 1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithDollarString() {
        ArtifactFixture foo = givenArtifact("foo").version("$1");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: $1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithVariableInComment() {
        ArtifactFixture foo = givenArtifact("foo").version("1");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1 # ${not} cool\n"
                + "# absolutely ${not} cool");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithUndefinedVariable() {
        ArtifactFixture foo = givenArtifact("foo").version("${undefined}");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${undefined}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithEscapedVariable() {
        ArtifactFixture foo = givenArtifact("${bar}", "org.foo", "foo-war").version("1");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  $${bar}:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo-war\n"
                + "    version: 1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldUpdateExistingWebArchive() {
        ArtifactFixture foo2 = givenArtifact("foo")
                .version("1").deployed()
                .and()
                .version("2");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 2\n");

        foo2.verifyRedeployed(audits);
    }


    @Test
    public void shouldNotRedeployWebArchiveWithSameNameAndChecksum() {
        givenArtifact("foo")
                .version("1").deployed()
                .and()
                .version("2");

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1\n");

        // #after(): no deploy operations
        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldDeployWebArchiveWithSameChecksumButDifferentName() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar", "org.foo", "foo-war").version("1").checksum(foo.checksum());

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  bar:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo-war\n"
                + "    version: 1\n"
        );

        // #after(): foo not undeployed
        bar.verifyDeployed(audits);
    }


    @Test
    public void shouldNotDeployWebArchiveWithSameNameButDifferentGroup() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar", foo.groupId(), foo.artifactId()).version("1");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  bar:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo\n"
                + "    version: 1\n"
        );

        // #after(): foo not undeployed
        bar.verifyDeployed(audits);
    }


    @Test
    public void shouldDeploySecondWebArchive() {
        givenArtifact("jolokia")
                .version("1.3.2").deployed()
                .and()
                .version("1.3.3");
        ArtifactFixture mockserver = givenArtifact("mockserver", "org.mock-server", "mockserver-war")
                .version("3.10.4");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n"
                + "  mockserver:\n"
                + "    group-id: org.mock-server\n"
                + "    artifact-id: mockserver-war\n"
                + "    version: 3.10.4\n");

        // #after(): jolokia not undeployed
        mockserver.verifyDeployed(audits);
    }


    @Test
    public void shouldUndeployWebArchiveWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed\n"
        );

        foo.verifyUndeployed(audits);
    }


    @Test
    public void shouldFailToUndeployWebArchiveWithoutVersion() {
        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "artifacts:\n"
                + "  foo-war:\n"
                + "    group-id: org.foo\n"
                + "    state: undeployed\n"));

        assertThat(thrown.getCause()).hasMessageContaining("version");
    }


    @Test
    public void shouldUndeployWebArchiveWithAnyVersionWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: '*'\n"
                + "    state: undeployed\n");

        foo.version("*").verifyUndeployed(audits);
    }


    @Test
    public void shouldRenameWebArchiveWithSameChecksumWhenManaged() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar", "org.foo", "foo-war").version("1");
        deployer.setManaged(true);

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  bar:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo-war\n"
                + "    version: 1\n"
        );

        verify(artifacts).undeploy(foo.deploymentName());
        verify(artifacts).deploy(bar.deploymentName(), bar.inputStream());
        assertThat(audits.getAudits()).containsExactly(
                bar.artifactAudit().added(),
                foo.artifactAudit().removed());
    }


    @Test
    public void shouldUndeployUnspecifiedWebArchiveWhenManaged() {
        givenArtifact("jolokia").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        deployer.setManaged(true);

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");

        // #after(): jolokia not undeployed
        mockserver.verifyUndeployed(audits);
    }


    @Test
    public void shouldDeployBundle() {
        givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("mockserver", "org.mock-server", "mockserver-war").version("3.10.4");
        givenArtifact("artifact-deployer-test", "should-deploy-bundle")
                .version("1", bundle)
                .containing(""
                        + "artifacts:\n"
                        + "  jolokia:\n"
                        + "    group-id: org.jolokia\n"
                        + "    artifact-id: jolokia-war\n"
                        + "    version: 1.3.2\n"
                        + "  mockserver:\n"
                        + "    group-id: org.mock-server\n"
                        + "    artifact-id: mockserver-war\n"
                        + "    version: 3.10.4\n");

        Audits audits = deployer.run(""
                + "artifacts:\n"
                + "  should-deploy-bundle:\n"
                + "    group-id: artifact-deployer-test\n"
                + "    type: bundle\n"
                + "    version: 1\n");

        // #after(): jolokia not re-deployed
        mockserver.verifyDeployed(audits);
    }


    // TODO shouldDeployBundleWithParams


    @Test
    public void shouldUndeployEverythingWhenManagedAndEmptyPlan() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        deployer.setManaged(true);

        Audits audits = deployer.run("---\n");

        verify(artifacts).undeploy(jolokia.deploymentName());
        verify(artifacts).undeploy(mockserver.deploymentName());
        assertThat(audits.getAudits()).containsExactly(
                jolokia.artifactAudit().removed(),
                mockserver.artifactAudit().removed());
    }
}
