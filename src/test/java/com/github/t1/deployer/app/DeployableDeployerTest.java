package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.app.ConfigurationPlan.ConfigurationPlanLoadingException;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.problem.WebApplicationApplicationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.t1.deployer.model.ArtifactType.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployableDeployerTest extends AbstractDeployerTest {
    @Test
    public void shouldDeployWebArchive() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
        );

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployEmptyDeployables() {
        Audits audits = deployer.apply(""
                + "deployables:\n");

        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldFailToDeployWebArchiveWithEmptyItem() {
        Throwable thrown = catchThrowable(() -> deployer.apply(""
                + "deployables:\n"
                + "  foo-war:\n"));

        assertThat(thrown.getCause()).hasMessageContaining("no config in deployable 'foo-war'");
    }


    @Test
    public void shouldFailToDeployWebArchiveWithoutVersion() {
        Throwable thrown = catchThrowable(() -> deployer.apply(""
                + "deployables:\n"
                + "  foo-war:\n"
                + "    group-id: org.foo\n"
                + "    state: deployed\n"));

        assertThat(thrown).isInstanceOf(ConfigurationPlanLoadingException.class);
        assertThat(thrown.getCause()).hasMessageContaining("failed (java.lang.NullPointerException): version");
    }


    @Test
    public void shouldFailToDeployBundleAsDeployable() {
        Throwable thrown = catchThrowable(() -> deployer.apply(""
                + "deployables:\n"
                + "  foo-war:\n"
                + "    type: bundle\n"
                + "    version: 1.0\n"
                + "    group-id: org.foo\n"));

        assertThat(thrown.getCause())
                .hasMessageContaining("a deployable may not be of type 'bundle'; use 'bundles' plan instead.");
    }


    @Test
    public void shouldDeployWebArchiveWithOtherName() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();
        ArtifactFixture bar = givenArtifact("bar", foo.groupId(), foo.artifactId()).version("1.3.2");

        Audits audits = deployer.apply(""
                + "deployables:\n"
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

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  ${fooArtifactId}:\n"
                + "    group-id: ${fooGroupId}\n"
                + "    version: ${fooVersion}\n");

        foo.verifyDeployed(audits);
    }


    @Test public void shouldFailToReplaceVariableWithNewline() { shouldFailToReplaceVariableWith("foo\nbar"); }

    @Test public void shouldFailToReplaceVariableWithOpeningCurly() { shouldFailToReplaceVariableWith("foo{bar"); }

    @Test public void shouldFailToReplaceVariableWithClosingCurly() { shouldFailToReplaceVariableWith("foo}bar"); }

    @Test public void shouldFailToReplaceVariableWithOnlyOpeningCurly() { shouldFailToReplaceVariableWith("{"); }

    @Test public void shouldFailToReplaceVariableWithCurlies() { shouldFailToReplaceVariableWith("{}"); }

    @Test public void shouldFailToReplaceVariableWithAsterisk() { shouldFailToReplaceVariableWith("foo*bar"); }

    private void shouldFailToReplaceVariableWith(String value) {
        systemProperties.given("foo", value);

        Throwable thrown = catchThrowable(() -> deployer.apply(""
                + "deployables:\n"
                + "  ${foo}:\n"
                + "    group-id: org.foo\n"
                + "    version: 1\n"));

        assertThat(thrown).hasMessageContaining("invalid character in variable value for [foo]");
    }


    @Test
    public void shouldDeployWebArchiveWithTwoVariablesInOneLine() {
        systemProperties.given("orgVar", "org");
        systemProperties.given("fooVar", "foo");
        ArtifactFixture foo = givenArtifact("foo").version("1");

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${orgVar}.${fooVar}\n"
                + "    version: 1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithCommentAfterVariable() {
        systemProperties.given("orgVar", "org");
        ArtifactFixture foo = givenArtifact("foo").version("1");

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${orgVar}.foo # cool\n"
                + "    version: 1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithDollarString() {
        ArtifactFixture foo = givenArtifact("foo").version("$1");

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: $1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithVariableInComment() {
        ArtifactFixture foo = givenArtifact("foo").version("1");

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1 # ${not} cool\n"
                + "# absolutely ${not} cool");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldFailToDeployWebArchiveWithUndefinedVariable() {
        Throwable thrown = catchThrowable(() -> deployer.apply(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${undefined}\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("unresolved variable key: undefined");
    }


    @Test
    public void shouldDeployWebArchiveWithEscapedVariable() {
        ArtifactFixture foo = givenArtifact("${bar}", "org.foo", "foo-war").version("1");

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  $${bar}:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo-war\n"
                + "    version: 1\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithToUpperAndToLowerCaseVariables() {
        systemProperties.given("foo", "Foo");
        ArtifactFixture foo = givenArtifact("FOO", "org.foo", "Foo").version("1.3.2");

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  ${toUpperCase(foo)}:\n"
                + "    group-id: org.${toLowerCase(foo)}\n"
                + "    artifact-id: ${foo}\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldFailToDeployWebArchiveWithUndefinedVariableFunction() {
        systemProperties.given("foo", "Foo");

        Throwable thrown = catchThrowable(() -> deployer.apply(""
                + "deployables:\n"
                + "  ${bar(foo)}:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo\n"
                + "    version: 1.3.2\n"));

        assertThat(thrown).hasMessageContaining("undefined variable function: [bar]");
    }

    @Test
    public void shouldFailToDeployWebArchiveWithUndefinedFunctionVariable() {
        Throwable thrown = catchThrowable(() -> deployer.apply(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${toLowerCase(undefined)}\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("unresolved variable key: undefined");
    }


    @Test
    public void shouldUpdateExistingWebArchive() {
        ArtifactFixture foo2 = givenArtifact("foo")
                .version("1").deployed()
                .and()
                .version("2");

        Audits audits = deployer.apply(""
                + "deployables:\n"
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

        Audits audits = deployer.apply(""
                + "org.foo:\n"
                + "  foo-war:\n"
                + "    version: 1\n");

        // #after(): no deploy operations
        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldDeployWebArchiveWithSameChecksumButDifferentName() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar", "org.foo", "foo-war").version("1").checksum(foo.getChecksum());

        Audits audits = deployer.apply(""
                + "deployables:\n"
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

        Audits audits = deployer.apply(""
                + "deployables:\n"
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

        Audits audits = deployer.apply(""
                + "deployables:\n"
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

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed\n"
        );

        foo.verifyRemoved(audits);
    }


    @Test
    public void shouldFailToUndeployWebArchiveWithoutVersion() {
        Throwable thrown = catchThrowable(() -> deployer.apply(""
                + "deployables:\n"
                + "  foo-war:\n"
                + "    group-id: org.foo\n"
                + "    state: undeployed\n"));

        assertThat(thrown.getCause()).hasMessageContaining("version");
    }


    @Test
    public void shouldUndeployWebArchiveWithAnyVersionWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();
        Checksum checksum = foo.getChecksum();

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: '*'\n"
                + "    state: undeployed\n");

        foo.version("*").checksum(checksum).verifyRemoved(audits);
    }


    @Test
    public void shouldRenameWebArchiveWithSameChecksumWhenManaged() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar", "org.foo", "foo-war").version("1");
        givenManaged("deployables");

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  bar:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo-war\n"
                + "    version: 1\n"
        );

        foo.verifyUndeployExecuted();
        bar.verifyAddExecuted();
        assertThat(audits.getAudits()).containsExactly(bar.addedAudit(), foo.removedAudit());
    }


    @Test
    public void shouldUndeployUnspecifiedWebArchiveWhenManaged() {
        givenArtifact("jolokia").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        givenManaged("deployables");

        Audits audits = deployer.apply(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");

        // #after(): jolokia not undeployed
        mockserver.verifyRemoved(audits);
    }


    @Test
    public void shouldDeployBundle() {
        givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("mockserver", "org.mock-server", "mockserver-war").version("3.10.4");
        givenArtifact(bundle, "artifact-deployer-test", "should-deploy-bundle")
                .version("1")
                .containing(""
                        + "deployables:\n"
                        + "  jolokia:\n"
                        + "    group-id: org.jolokia\n"
                        + "    artifact-id: jolokia-war\n"
                        + "    version: 1.3.2\n"
                        + "  mockserver:\n"
                        + "    group-id: org.mock-server\n"
                        + "    artifact-id: mockserver-war\n"
                        + "    version: 3.10.4\n");

        Audits audits = deployer.apply(""
                + "bundles:\n"
                + "  should-deploy-bundle:\n"
                + "    group-id: artifact-deployer-test\n"
                + "    version: 1\n");

        // #after(): jolokia not re-deployed
        mockserver.verifyAddExecuted();
        assertThat(audits.getAudits()).containsExactly(
                mockserver.addedAudit());
    }


    @Test
    public void shouldDeployBundleWithSystemParam() {
        systemProperties.given("jolokia.version", "1.3.3");
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "should-deploy-bundle")
                .version("1")
                .containing(""
                        + "deployables:\n"
                        + "  jolokia:\n"
                        + "    group-id: org.jolokia\n"
                        + "    artifact-id: jolokia-war\n"
                        + "    version: ${jolokia.version}\n");

        Audits audits = deployer.apply(""
                + "bundles:\n"
                + "  should-deploy-bundle:\n"
                + "    group-id: artifact-deployer-test\n"
                + "    version: 1\n");

        jolokia.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployBundleWithPassedParam() {
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "should-deploy-bundle")
                .version("1")
                .containing(""
                        + "deployables:\n"
                        + "  jolokia:\n"
                        + "    group-id: org.jolokia\n"
                        + "    artifact-id: jolokia-war\n"
                        + "    version: ${jolokia.version}\n");

        Audits audits = deployer.apply(""
                + "bundles:\n"
                + "  should-deploy-bundle:\n"
                + "    group-id: artifact-deployer-test\n"
                + "    version: 1\n"
                + "    vars:\n"
                + "      jolokia.version: 1.3.3\n");

        jolokia.verifyDeployed(audits);
    }

    @Test
    public void shouldUndeployEverythingWhenManagedAndEmptyPlan() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        givenManaged("deployables", "loggers", "log-handlers");

        Audits audits = deployer.apply("---\n");

        jolokia.verifyUndeployExecuted();
        mockserver.verifyUndeployExecuted();
        assertThat(audits.getAudits()).containsExactly(jolokia.removedAudit(), mockserver.removedAudit());
    }
}
