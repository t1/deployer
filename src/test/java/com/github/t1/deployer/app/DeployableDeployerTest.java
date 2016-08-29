package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTest.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.Variables.UnresolvedVariableException;
import com.github.t1.problem.WebApplicationApplicationException;
import org.junit.Test;

import java.net.InetAddress;

import static com.github.t1.deployer.model.ArtifactType.*;
import static org.assertj.core.api.Assertions.*;

public class DeployableDeployerTest extends AbstractDeployerTest {
    private static final Checksum UNKNOWN_CHECKSUM = Checksum.ofHexString("9999999999999999999999999999999999999999");

    @Test
    public void shouldDeployWebArchive() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
        );

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithCorrectChecksum() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
                + "    checksum: " + foo.getChecksum() + "\n"
        );

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldFailToDeployWebArchiveWithIncorrectChecksum() {
        givenArtifact("foo").version("1.3.2");

        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
                + "    checksum: 2ea859259d7a9e270b4484facdcba5fe3f1f7578\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("Repository checksum [face000097269fd347ce0e93059890430c01f17f]"
                        + " does not match planned checksum [2ea859259d7a9e270b4484facdcba5fe3f1f7578]");
    }

    @Test
    public void shouldUpdateWebArchiveWithCorrectChecksum() {
        givenArtifact("foo").version("1.3.1").deployed();
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
                + "    checksum: " + foo.getChecksum() + "\n"
        );

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldFailToUpdateWebArchiveWithIncorrectChecksum() {
        givenArtifact("foo").version("1.3.1").deployed();
        givenArtifact("foo").version("1.3.2");

        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
                + "    checksum: 2ea859259d7a9e270b4484facdcba5fe3f1f7578\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("Repository checksum [face000097269fd347ce0e93059890430c01f17f] "
                        + "does not match planned checksum [2ea859259d7a9e270b4484facdcba5fe3f1f7578]");
    }

    @Test
    public void shouldFailToCheckWebArchiveWithIncorrectChecksum() {
        givenArtifact("foo").version("1.3.1").deployed();

        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.1\n"
                + "    checksum: 2ea859259d7a9e270b4484facdcba5fe3f1f7578\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("Repository checksum [face000094d353f082e6939015af81d263ba0f8f] "
                        + "does not match planned checksum [2ea859259d7a9e270b4484facdcba5fe3f1f7578]");
    }

    @Test
    public void shouldDeployEmptyDeployables() {
        Audits audits = deploy(""
                + "deployables:\n");

        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldFailToDeployWebArchiveWithEmptyItem() {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo-war:\n"));

        assertThat(thrown).hasStackTraceContaining("no config in deployable 'foo-war'");
    }


    @Test
    public void shouldFailToDeployWebArchiveWithoutVersion() {
        givenArtifact("foo").version("1.3.2");

        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    state: deployed\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("artifact not found: «deployment:foo:deployed:org.foo:foo:CURRENT:war»");
    }


    @Test
    public void shouldFailToDeployBundleAsDeployable() {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo-war:\n"
                + "    type: bundle\n"
                + "    version: 1.0\n"
                + "    group-id: org.foo\n"));

        assertThat(thrown)
                .hasStackTraceContaining("a deployable may not be of type 'bundle'; use 'bundles' plan instead.");
    }


    @Test
    public void shouldDeployWebArchiveWithOtherName() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();
        ArtifactFixture bar = givenArtifact("bar", foo.groupId(), foo.artifactId()).version("1.3.2");

        Audits audits = deploy(""
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

        Audits audits = deploy(""
                + "deployables:\n"
                + "  ${fooArtifactId}:\n"
                + "    group-id: ${fooGroupId}\n"
                + "    version: ${fooVersion}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithFirstOfTwoOrVariables() {
        systemProperties.given("fooVersion", "1.3.2");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${fooVersion or version}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithSecondOfTwoOrVariables() {
        systemProperties.given("version", "1.3.2");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${fooVersion or version}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithFirstOfThreeOrFunctionVariables() {
        systemProperties.given("fooName", "FOO");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  ${toLowerCase(fooName) or toLowerCase(barName) or toLowerCase(bazName)}:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithSecondOfThreeOrFunctionVariables() {
        systemProperties.given("barName", "FOO");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  ${toLowerCase(fooName) or toLowerCase(barName) or toLowerCase(bazName)}:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithThirdOfThreeOrFunctionVariables() {
        systemProperties.given("bazName", "FOO");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  ${toLowerCase(fooName) or toLowerCase(barName) or toLowerCase(bazName)}:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithHostNameFunction() throws Exception {
        String hostName = InetAddress.getLocalHost().getHostName().split("\\.")[0];
        ArtifactFixture foo = givenArtifact("foo").groupId(hostName).version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${hostName()}\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldFailToResolveHostNameFunctionWithParameter() throws Exception {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${hostName(os.name)}\n"
                + "    version: 1.3.2\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("undefined variable function with 1 params: [hostName]");
    }


    @Test
    public void shouldDeployWebArchiveWithDomainNameFunction() throws Exception {
        String domainName = InetAddress.getLocalHost().getHostName().split("\\.", 2)[1];
        ArtifactFixture foo = givenArtifact("foo").groupId(domainName).version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${domainName()}\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldFailToResolveDomainNameFunctionWithParameter() throws Exception {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${domainName(os.name)}\n"
                + "    version: 1.3.2\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("undefined variable function with 1 params: [domainName]");
    }


    @Test
    public void shouldDeployWebArchiveWithStringLiteral() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${undefined or «org.foo»}\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithOrParam() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${undefined0 or toLowerCase(undefined1 or «org.FOO»)}\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithRegexSuffix() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${regex(«org.foo01», «(.*?)\\d*»)}\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithRegexPrefix() throws Exception {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${regex(«qa.org.foo», «(?:qa\\.)(.*?)»)}\n"
                + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test public void shouldFailToReplaceVariableWithNewline() { shouldFailToReplaceVariableWith("foo\nbar"); }

    @Test public void shouldFailToReplaceVariableWithOpeningCurly() { shouldFailToReplaceVariableWith("foo{bar"); }

    @Test public void shouldFailToReplaceVariableWithClosingCurly() { shouldFailToReplaceVariableWith("foo}bar"); }

    @Test public void shouldFailToReplaceVariableWithOnlyOpeningCurly() { shouldFailToReplaceVariableWith("{"); }

    @Test public void shouldFailToReplaceVariableWithCurlies() { shouldFailToReplaceVariableWith("{}"); }

    @Test public void shouldFailToReplaceVariableWithAsterisk() { shouldFailToReplaceVariableWith("foo*bar"); }

    @Test public void shouldFailToReplaceFunctionWithLeadingStar() { shouldFailToReplaceVariableWith("*foo(bar)"); }

    private void shouldFailToReplaceVariableWith(String value) {
        systemProperties.given("foo", value);

        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  ${foo}:\n"
                + "    group-id: org.foo\n"
                + "    version: 1\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("invalid character in variable value for [foo]");
    }


    @Test
    public void shouldDeployWebArchiveWithTwoVariablesInOneLine() {
        systemProperties.given("orgVar", "org");
        systemProperties.given("fooVar", "foo");
        ArtifactFixture foo = givenArtifact("foo").version("1");

        Audits audits = deploy(""
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

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${orgVar}.foo # cool\n"
                + "    version: 1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithDollarString() {
        ArtifactFixture foo = givenArtifact("foo").version("$1");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: $1\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithVariableInComment() {
        ArtifactFixture foo = givenArtifact("foo").version("1");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1 # ${not} cool\n"
                + "# absolutely ${not} cool");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldFailToDeployWebArchiveWithUndefinedVariable() {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${undefined}\n"));

        assertThat(thrown)
                .isInstanceOf(UnresolvedVariableException.class)
                .hasMessageContaining("unresolved variable expression: undefined");
    }


    @Test
    public void shouldDeployWebArchiveWithEscapedVariable() {
        ArtifactFixture foo = givenArtifact("${bar}", "org.foo", "foo-war").version("1");

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  ${bar(foo)}:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo\n"
                + "    version: 1.3.2\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("undefined variable function with 1 params: [bar]");
    }

    @Test
    public void shouldFailToDeployWebArchiveWithVariableFunctionMissingParam() {
        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  ${toLowerCase()}:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: foo\n"
                + "    version: 1.3.2\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("undefined variable function with 0 params: [toLowerCase]");
    }

    @Test
    public void shouldFailToDeployWebArchiveWithUndefinedFunctionVariable() {
        Throwable thrown = catchThrowable(() ->
                deploy(""
                        + "deployables:\n"
                        + "  foo:\n"
                        + "    group-id: org.foo\n"
                        + "    version: ${toLowerCase(undefined)}\n"));

        assertThat(thrown)
                .isInstanceOf(UnresolvedVariableException.class)
                .hasMessageContaining("unresolved variable expression: toLowerCase(undefined)");
    }


    @Test
    public void shouldUpdateExistingWebArchive() {
        ArtifactFixture foo2 = givenArtifact("foo")
                .version("1").deployed()
                .and()
                .version("2");

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed\n"
        );

        foo.verifyRemoved(audits);
    }


    @Test
    public void shouldUndeployWebArchiveWithoutVersionAndGroupId() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    state: undeployed\n");

        foo.verifyRemoved(audits);
    }


    @Test
    public void shouldUndeployWebArchiveWithOtherVersionWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 999\n"
                + "    state: undeployed\n");

        foo.verifyRemoved(audits);
    }

    @Test
    public void shouldUndeployWebArchiveWithOtherGroupIdWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.bar\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed\n");

        foo.verifyRemoved(audits);
    }

    @Test
    public void shouldUndeployWebArchiveWithOtherArtifactIdWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    artifact-id: bar\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed\n");

        foo.verifyRemoved(audits);
    }

    @Test
    public void shouldFailToUndeployWebArchiveWithWrongChecksum() {
        givenArtifact("foo").version("1.3.2").deployed();

        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
                + "    checksum: " + UNKNOWN_CHECKSUM + "\n"
                + "    state: undeployed\n"
        ));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("Planned to undeploy artifact with checksum [" + UNKNOWN_CHECKSUM + "] "
                        + "but deployed is [face000097269fd347ce0e93059890430c01f17f]");
    }


    @Test
    public void shouldRenameWebArchiveWithSameChecksumWhenManaged() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar", "org.foo", "foo-war").version("1");
        givenManaged("deployables");

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy(""
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

        Audits audits = deploy("---\n");

        jolokia.verifyUndeployExecuted();
        mockserver.verifyUndeployExecuted();
        assertThat(audits.getAudits()).containsExactly(jolokia.removedAudit(), mockserver.removedAudit());
    }
}
