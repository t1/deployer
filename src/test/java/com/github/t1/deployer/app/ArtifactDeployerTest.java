package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixture.VersionFixture;
import com.github.t1.deployer.container.DeploymentName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.ConstraintViolation;

import static com.github.t1.deployer.model.ArtifactType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactDeployerTest extends AbstractDeployerTest {
    private static final DeploymentName FOO_WAR = new DeploymentName("foo-war");
    private static final DeploymentName BAR = new DeploymentName("bar");
    private static final DeploymentName MOCKSERVER = new DeploymentName("mockserver");

    @Test
    public void shouldDeployWebArchive() {
        VersionFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1.3.2\n"
        );

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldFailToDeployWebArchiveWithEmptyItem() {
        Throwable thrown = catchThrowable(() ->
                deployer.run(""
                        + "org.foo:\n"
                        + "  foo-war:\n"));

        assertThat(unpackViolations(thrown))
                .extracting(ConstraintViolation::getMessage, v -> v.getRootBeanClass().getName())
                .containsExactly(tuple("may not be null", "com.github.t1.deployer.app.Deployer$Run$1NotNullContainer"));
    }


    @Test
    public void shouldFailToDeployWebArchiveWithoutVersion() {
        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    state: deployed\n"));

        assertThat(unpackViolations(thrown))
                .extracting(ConstraintViolation::getMessage, ArtifactDeployerTest::pathString)
                .containsExactly(tuple("may not be null", "version"));
    }


    @Test
    public void shouldDeployWebArchiveWithOtherName() {
        VersionFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1.3.2\n"
                + "    name: bar"
        );

        foo.named("bar").verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithVariables() {
        systemProperties.given("fooGroupId", "org.foo");
        systemProperties.given("fooArtifactId", FOO_WAR);
        systemProperties.given("fooVersion", "1.3.2");
        VersionFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deployer.run(""
                + "${fooGroupId}:\n"
                + "  ${fooArtifactId}:\n"
                + "    version: ${fooVersion}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithTwoVariablesInOneLine() {
        systemProperties.given("orgVar", "org");
        systemProperties.given("fooVar", "foo");
        VersionFixture foo = givenArtifact("foo").version("1");

        Audits audits = deployer.run(""
                + "${orgVar}.${fooVar}:\n"
                + "  foo-war:\n"
                + "    version: 1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithCommentAfterVariable() {
        systemProperties.given("orgVar", "org");
        VersionFixture foo = givenArtifact("foo").version("1");

        Audits audits = deployer.run(""
                + "${orgVar}.foo: # cool\n"
                + "  foo-war:\n"
                + "    version: 1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithDollarString() {
        VersionFixture foo = givenArtifact("foo").version("$1");

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: $1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithVariableInComment() {
        VersionFixture foo = givenArtifact("foo").version("1");

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1 # ${not} cool\n"
                + "# absolutely ${not} cool");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithUndefinedVariable() {
        VersionFixture foo = givenArtifact("foo").version("${undefined}");

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: ${undefined}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithEscapedVariable() {
        VersionFixture foo = givenArtifact("foo").named("${bar}").version("1");

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1\n"
                + "    name: $${bar}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldUpdateExistingWebArchive() {
        VersionFixture foo2 = givenArtifact("foo")
                .version("1").deployed()
                .and()
                .version("2");

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
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
        assertThat(audits.asList()).isEmpty();
    }


    @Test
    public void shouldDeployWebArchiveWithSameChecksumButDifferentName() {
        VersionFixture foo = givenArtifact("foo").version("1").deployed();
        VersionFixture bar = givenArtifact("foo").named(BAR.getValue()).version("1").checksum(foo.checksum());

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1\n"
                + "    name: bar\n");

        // #after(): foo not undeployed
        bar.verifyDeployed(audits);
    }


    @Test
    public void shouldDeploySecondWebArchive() {
        givenArtifact("jolokia")
                .version("1.3.2").deployed()
                .and()
                .version("1.3.3");
        VersionFixture mockserver = givenArtifact("org.mock-server", "mockserver-war")
                .named("mockserver")
                .version("3.10.4");

        Audits audits = deployer.run(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"
                + "org.mock-server:\n"
                + "  mockserver-war:\n"
                + "    name: mockserver\n"
                + "    version: 3.10.4\n");

        // #after(): jolokia not undeployed
        mockserver.verifyDeployed(audits);
    }


    @Test
    public void shouldUndeployWebArchiveWhenStateIsUndeployed() {
        VersionFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed\n"
        );

        foo.verifyUndeployed(audits);
    }


    @Test
    public void shouldFailToUndeployWebArchiveWithoutVersion() {
        Throwable thrown = catchThrowable(() -> deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    state: undeployed\n"));

        assertThat(unpackViolations(thrown))
                .extracting(ConstraintViolation::getMessage, ArtifactDeployerTest::pathString)
                .containsExactly(tuple("may not be null", "version"));
    }


    @Test
    public void shouldUndeployWebArchiveWithAnyVersionWhenStateIsUndeployed() {
        VersionFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: '*'\n"
                + "    state: undeployed\n");

        foo.version("*").verifyUndeployed(audits);
    }


    @Test
    public void shouldRenameWebArchiveWithSameChecksumWhenManaged() {
        VersionFixture foo = givenArtifact("foo").version("1").deployed();
        VersionFixture bar = givenArtifact("foo").named(BAR.getValue()).version("1");
        deployer.setManaged(true);

        Audits audits = deployer.run(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1\n"
                + "    name: bar\n");

        verify(deployments).undeploy(foo.deploymentName());
        verify(deployments).deploy(bar.deploymentName(), bar.inputStream());
        assertThat(audits.asList()).containsExactly(
                bar.artifactAudit().added(),
                foo.artifactAudit().removed());
    }


    @Test
    public void shouldUndeployUnspecifiedWebArchiveWhenManaged() {
        givenArtifact("jolokia").version("1.3.2").deployed();
        VersionFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        deployer.setManaged(true);

        Audits audits = deployer.run(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n");

        // #after(): jolokia not undeployed
        mockserver.verifyUndeployed(audits);
    }


    @Test
    public void shouldDeployBundle() {
        givenArtifact("jolokia").version("1.3.2").deployed();
        VersionFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4");
        givenArtifact("artifact-deployer-test", "should-deploy-bundle").version("1", bundle).containing(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"
                + "org.mock-server:\n"
                + "  mockserver-war:\n"
                + "    version: 3.10.4\n");

        Audits audits = deployer.run(""
                + "artifact-deployer-test:\n"
                + "  should-deploy-bundle:\n"
                + "    type: bundle\n"
                + "    version: 1\n");

        // #after(): jolokia not re-deployed
        mockserver.verifyDeployed(audits);
    }


    // TODO shouldDeployBundleWithParams


    @Test
    public void shouldUndeployEverythingWhenManagedAndEmptyPlan() {
        VersionFixture jolokia = givenArtifact("jolokia").version("1.3.2").deployed();
        VersionFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        deployer.setManaged(true);

        Audits audits = deployer.run("---\n");

        verify(deployments).undeploy(jolokia.deploymentName());
        verify(deployments).undeploy(mockserver.deploymentName());
        assertThat(audits.asList()).containsExactly(
                jolokia.artifactAudit().removed(),
                mockserver.artifactAudit().removed());
    }
}
