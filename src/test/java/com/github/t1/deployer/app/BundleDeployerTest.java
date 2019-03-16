package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTests.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.Expressions.UnresolvedVariableException;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.deployer.tools.CipherFacade;
import com.github.t1.deployer.tools.KeyStoreConfig;
import com.github.t1.problem.WebApplicationApplicationException;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.net.InetAddress;

import static com.github.t1.deployer.app.Trigger.post;
import static com.github.t1.deployer.model.ArtifactType.bundle;
import static com.github.t1.deployer.model.ArtifactType.ear;
import static com.github.t1.deployer.model.ArtifactType.jar;
import static com.github.t1.deployer.model.Expressions.domainName;
import static com.github.t1.deployer.model.Expressions.hostName;
import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.deployer.testtools.TestData.VERSION;
import static com.github.t1.log.LogLevel.DEBUG;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class BundleDeployerTest extends AbstractDeployerTests {
    private static final KeyStoreConfig KEYSTORE = new KeyStoreConfig()
        .setPath("src/test/resources/test.keystore")
        .setType("jceks")
        .setPass("changeit");

    private final CipherFacade cipher = new CipherFacade();

    private String encrypt(String plain) { return cipher.encrypt(plain, boundary.keyStore); }

    private static final Checksum UNKNOWN_CHECKSUM = Checksum.ofHexString("9999999999999999999999999999999999999999");

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
        givenConfiguredVariable("fooGroupId", "org.foo");
        givenConfiguredVariable("fooArtifactId", "foo");
        givenConfiguredVariable("fooVersion", "1.3.2");
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
        givenConfiguredVariable("fooVersion", "1.3.2");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${fooVersion or barVersion}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithSecondOfTwoOrVariables() {
        givenConfiguredVariable("barVersion", "1.3.2");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${fooVersion or barVersion}\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithFirstOfThreeOrFunctionVariables() {
        givenConfiguredVariable("fooName", "FOO");
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
        givenConfiguredVariable("barName", "FOO");
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
        givenConfiguredVariable("bazName", "FOO");
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
    public void shouldFailToResolveHostNameFunctionWithParameter() {
        Throwable thrown = catchThrowable(() -> deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${hostName(os.name)}\n"
            + "    version: 1.3.2\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("undefined function [hostName] with 1 params");
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
    public void shouldFailToResolveDomainNameFunctionWithParameter() {
        Throwable thrown = catchThrowable(() -> deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${domainName(os.name)}\n"
            + "    version: 1.3.2\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("undefined function [domainName] with 1 params");
    }


    @Test
    public void shouldDeployWebArchiveWithStringLiteral() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${«org.foo»}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithStringLiteralContainingSingleQuotes() {
        ArtifactFixture foo = givenArtifact("foo").groupId("foo'bar'baz").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${«foo'bar'baz»}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithStringLiteralContainingDoubleQuotes() {
        ArtifactFixture foo = givenArtifact("foo").groupId("foo\"bar\"baz").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${«foo\"bar\"baz»}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithStringLiteralContainingGuillemetQuotes() {
        ArtifactFixture foo = givenArtifact("foo").groupId("foo«bar»baz").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${«foo«bar»baz»}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithOrParam() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${undefined0 or toLowerCase(undefined1 or «org.FOO»)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithRegexSuffix() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${regex(«org.foo01», «(.*?)\\d*»)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithRegexPrefix() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${regex(«qa.org.foo», «(?:qa\\.)(.*?)»)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithRegexVariable() {
        givenConfiguredVariable("my-regex", "(?:qa\\.)(.*?)");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${regex(«qa.org.foo», my-regex)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithPublicKeyEncryptedVersion() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(KEYSTORE.withAlias("keypair"));

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithPublicKeyEncryptedVersionUsingAliasParameter() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(KEYSTORE);

        String secret = cipher.encrypt(foo.getVersion().getValue(), boundary.keyStore.withAlias("keypair"));
        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + secret + "», «keypair»)}\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithSecretKeyEncryptedVersion() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(KEYSTORE.withAlias("secretkey"));

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithEncryptedVersionUsingDefaultKeystoreType() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(new KeyStoreConfig()
            .setPath("src/test/resources/jks.keystore")
            .setPass("changeit")
            .setAlias("keypair"));

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldFailToDeployWebArchiveWithEncryptedVariableButWithoutKeystoreConfig() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Throwable thrown = catchThrowable(() -> deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n"));

        assertThat(thrown).hasMessageContaining("no key-store configured");
    }

    @Test
    public void shouldFailToDeployWebArchiveWithEncryptedVariableButWithoutPath() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(new KeyStoreConfig().setType("jceks").setPass("changeit").withAlias("keypair"));

        Throwable thrown = catchThrowable(() -> deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n"));

        assertThat(thrown).hasMessageContaining("no key-store path configured");
    }


    @Test
    public void shouldDeployWebArchiveWithFirstSwitch() {
        givenConfiguredVariable("bar", "A");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.1");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch(bar)\n"
            + "    A: «1.3.1»\n"
            + "    B: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithMiddleSwitch() {
        givenConfiguredVariable("bar", "B");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch(bar)\n"
            + "    A: «1.3.1»\n"
            + "    B: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithLastSwitch() {
        givenConfiguredVariable("bar", "C");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.3");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch(bar)\n"
            + "    A: «1.3.1»\n"
            + "    B: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n");

        foo.verifyDeployed(audits);
    }

    @Test
    public void shouldFailToDeployWebArchiveWithSwitchWithoutHead() {
        Throwable thrown = catchThrowable(() -> deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch\n"
            + "    A: «1.3.1»\n"
            + "    B: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n"));

        assertThat(thrown)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unmatched brackets for switch statement");
    }

    @Test
    public void shouldFailToDeployWebArchiveWithSwitchWithUnsetVariable() {
        Throwable thrown = catchThrowable(() -> deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch(bar)\n"
            + "    A: «1.3.1»\n"
            + "    X: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n"));

        assertThat(thrown)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no variable defined in switch header: 'bar'");
    }

    @Test
    public void shouldFailToDeployWebArchiveWithSwitchWithoutMatchingCase() {
        givenConfiguredVariable("bar", "B");

        Throwable thrown = catchThrowable(() -> deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch(bar)\n"
            + "    A: «1.3.1»\n"
            + "    X: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n"));

        assertThat(thrown)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no case label for 'B' in switch statement");
    }

    @Test
    public void shouldFailToDeployWebArchiveWithSwitchWithoutMatchingCaseButSomethingWithAPrefix() {
        givenConfiguredVariable("bar", "B");

        Throwable thrown = catchThrowable(() -> deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch(bar)\n"
            + "    A: «1.3.1»\n"
            + "    notB: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n"));

        assertThat(thrown)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no case label for 'B' in switch statement");
    }


    @Test
    public void shouldDeployWebArchiveWithEncryptedVersionUsingSwitch() {
        givenConfiguredVariable("stage", "QA");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(new KeyStoreConfig()
            .setPath("src/test/resources/jks.keystore")
            .setPass("changeit")
            .setAlias("keypair"));

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${decrypt(switch(stage)\n"
            + "      TEST: «test-dummy»\n"
            + "      QA: «" + encrypt(foo.getVersion().getValue()) + "»\n"
            + "      PROD: «prod-dummy»\n"
            + "      )}\"\n");

        foo.verifyDeployed(audits);
    }


    @Test public void shouldFailToReplaceVariableValueWithNewline() {
        shouldFailToReplaceVariableValueWith("foo\nbar");
    }

    @Test public void shouldFailToReplaceVariableValueWithTab() { shouldFailToReplaceVariableValueWith("\tfoo"); }

    private void shouldFailToReplaceVariableValueWith(String value) {
        givenConfiguredVariable("foo", value);

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
        givenConfiguredVariable("orgVar", "org");
        givenConfiguredVariable("fooVar", "foo");
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
        givenConfiguredVariable("orgVar", "org");
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
            + "    group-id: ${undefined}\n"
            + "    version: 1.2\n"));

        assertThat(thrown)
            .hasRootCauseExactlyInstanceOf(UnresolvedVariableException.class)
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
    public void shouldDeployWebArchiveWithChangedCaseVariables() {
        givenConfiguredVariable("foo", "foo");
        ArtifactFixture foo = givenArtifact("FOO", "org.foo", "Foo").version("1.3.2");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  ${toUpperCase(foo)}:\n"
            + "    group-id: org.${toLowerCase(foo)}\n"
            + "    artifact-id: ${toInitCap(foo)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed(audits);
    }


    @Test
    public void shouldFailToDeployWebArchiveWithUndefinedVariableFunction() {
        givenConfiguredVariable("foo", "Foo");

        Throwable thrown = catchThrowable(() -> deploy(""
            + "deployables:\n"
            + "  ${bar(foo)}:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo\n"
            + "    version: 1.3.2\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("undefined function [bar] with 1 params");
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
            .hasMessageContaining("undefined function [toLowerCase] with 0 params");
    }

    @Test
    public void shouldFailToDeployWebArchiveWithUndefinedFunctionVariable() {
        Throwable thrown = catchThrowable(() ->
            deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${toLowerCase(undefined)}\n"
                + "    version: 1.2\n"));

        assertThat(thrown)
            .hasRootCauseExactlyInstanceOf(UnresolvedVariableException.class)
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
        ArtifactFixture mockServer = givenArtifact("mockserver", "org.mock-server", "mockserver-war")
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
        mockServer.verifyDeployed(audits);
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

        bar.verifyDeployed(audits);
        foo.verifyUndeployExecuted();
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
    public void shouldUndeployUnspecifiedWebArchiveWhenAllManaged() {
        givenArtifact("jolokia").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        givenManaged("all");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    group-id: org.jolokia\n"
            + "    version: 1.3.2\n");

        // #after(): jolokia not undeployed
        mockserver.verifyRemoved(audits);
    }


    @Test
    public void shouldDeployWebArchiveWithConfiguredVariable() {
        givenConfiguredVariable("v", "1.3.3");
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    group-id: org.jolokia\n"
            + "    artifact-id: jolokia-war\n"
            + "    version: ${v}");

        jolokia.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployWebArchiveWithDefaultGroupId() {
        givenConfiguredVariable("default.group-id", "org.jolokia");
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    artifact-id: jolokia-war\n"
            + "    version: 1.3.3\n");

        jolokia.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployJar() {
        ArtifactFixture postgresql = givenArtifact(jar, "postgresql").version("9.4.1207");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  postgresql:\n"
            + "    group-id: org.postgresql\n"
            + "    version: 9.4.1207\n"
            + "    type: jar\n");

        postgresql.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployJarWithDefaultType() {
        givenConfiguredVariable("default.deployable-type", "jar");
        ArtifactFixture postgresql = givenArtifact(jar, "postgresql").version("9.4.1207");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  postgresql:\n"
            + "    group-id: org.postgresql\n"
            + "    version: 9.4.1207\n");

        postgresql.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployEar() {
        ArtifactFixture postgresql = givenArtifact(ear, "foo").version("1");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1\n"
            + "    type: ear\n");

        postgresql.verifyDeployed(audits);
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

    @Test
    public void shouldDeployLatestWebArchive() {
        ArtifactFixture latest = givenArtifact("foo")
            .version("1.3.2").and()
            .version("1.4.4").and()
            .version("2.0.0-SNAPSHOT").and()
            .version("1.5.0-SNAPSHOT").and()
            .version("1.4.11");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: LATEST\n"
        );

        latest.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployUnstableWebArchive() {
        ArtifactFixture unstable = givenArtifact("foo")
            .version("1.3.2").and()
            .version("1.4.4").and()
            .version("1.4.11").and()
            .version("1.5.0-SNAPSHOT").and()
            .version("2.0.0-SNAPSHOT");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: UNSTABLE\n"
        );

        unstable.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployUnstableButReleasedWebArchive() {
        ArtifactFixture unstable = givenArtifact("foo")
            .version("1.3.2").and()
            .version("1.4.4").and()
            .version("1.4.11").and()
            .version("1.5.0-SNAPSHOT").and()
            .version("2.0.0-SNAPSHOT").and()
            .version("2.0.0");

        Audits audits = deploy(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: UNSTABLE\n"
        );

        unstable.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployBundle() {
        givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("mockserver", "org.mock-server", "mockserver-war").version("3.10.4");
        givenArtifact(bundle, "artifact-deployer-test", "some-bundle")
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
            + "  some-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n");

        // #after(): jolokia not re-deployed
        mockserver.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployBundleWithSystemParam() {
        givenConfiguredVariable("jolokia.version", "1.3.3");
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "bundle-with-system-param")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: ${jolokia.version}\n");

        Audits audits = deploy(""
            + "bundles:\n"
            + "  bundle-with-system-param:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n");

        jolokia.verifyDeployed(audits);
    }

    @Test
    public void shouldDeployBundleWithPassedParam() {
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "bundle-with-passed-param")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: ${v}\n");

        Audits audits = deploy(""
            + "bundles:\n"
            + "  bundle-with-passed-param:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n"
            + "        v: 1.3.3\n");

        jolokia.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployBundleWithName() {
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "bundle-with-name")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.3\n");

        Audits audits = deploy(""
            + "bundles:\n"
            + "  bundle-with-name:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n");

        jolokia.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployLatestBundle() {
        ArtifactFixture latest = givenArtifact("jolokia", "org.jolokia", "jolokia-war")
            .version("1.3.2").and()
            .version("2.0.0-SNAPSHOT").and()
            .version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "latest-bundle")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: LATEST\n");

        Audits audits = deploy(""
            + "bundles:\n"
            + "  latest-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n");

        latest.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployUnstableBundle() {
        ArtifactFixture unstable = givenArtifact("jolokia", "org.jolokia", "jolokia-war")
            .version("1.3.2").and()
            .version("1.3.3").and()
            .version("2.0.0-SNAPSHOT");
        givenArtifact(bundle, "artifact-deployer-test", "unstable-bundle")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: UNSTABLE\n");

        Audits audits = deploy(""
            + "bundles:\n"
            + "  unstable-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n");

        unstable.verifyDeployed(audits);
    }


    @Test
    public void shouldDeployBundleWithDefaultVersionExpression() {
        givenConfiguredVariable("jolokia.version", "1.3.2");
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.2");
        givenArtifact(bundle, "artifact-deployer-test", "expression-bundle")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: ${version}\n");

        Audits audits = deploy(""
            + "bundles:\n"
            + "  expression-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n"
            + "        version: ${jolokia.version}\n");

        jolokia.verifyDeployed(audits);
    }


    @Test
    public void shouldNotDeployBundleWithDefaultCurrentVersionExpression() {
        givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.2").deployed();
        givenArtifact(bundle, "artifact-deployer-test", "current-expression-bundle")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: ${version}\n");

        Audits audits = deploy(""
            + "bundles:\n"
            + "  current-expression-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n"
            + "        version: ${jolokia.version}\n");

        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldFailToDeployBundleWithoutName() {
        givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "bundle-without-name")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.3\n");

        Throwable throwable = catchThrowable(() -> deploy(""
            + "bundles:\n"
            + "  bundle-without-name:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"));

        assertThat(throwable)
            .hasRootCauseExactlyInstanceOf(UnresolvedVariableException.class)
            .hasMessageContaining("name");
    }


    @Test
    public void shouldFailToDeployDefaultRootBundleWithoutConfigFile() {
        boundary.useDefaultConfig = true;

        Throwable thrown = catchThrowable(() -> boundary.apply(post, ImmutableMap.of(VERSION, "1.2")));

        assertThat(thrown).hasMessageContaining(
            "applying the default root bundle is only allowed when there is a configuration file");
    }

    @Test
    public void shouldDeployDefaultRootBundle() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");

        boundary.apply(post, ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed(boundary.audits);
    }

    @Test
    public void shouldDeployDefaultRootBundleWithDefaultGroupId() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenConfiguredVariable("default.group-id", "mygroup");
        givenArtifact(bundle, "dummy", "mygroup", hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");

        boundary.apply(post, ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed(boundary.audits);
    }

    @Test
    public void shouldDeployDefaultRootBundleWithConfiguredRootBundleArtifactId() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), "foo")
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");
        givenConfiguredRootBundle("artifact-id", "foo");

        boundary.apply(post, ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed(boundary.audits);
    }

    @Test
    public void shouldDeployDefaultRootBundleWithConfiguredRootBundleArtifactIdExpression() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");
        givenConfiguredRootBundle("artifact-id", "${hostName()}");

        boundary.apply(post, ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed(boundary.audits);
    }

    @Test
    public void shouldDeployDefaultRootBundleWithConfiguredRootBundleGroupId() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", "my.other.group", hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");
        givenConfiguredRootBundle("group-id", "my.other.group");

        boundary.apply(post, ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed(boundary.audits);
    }

    @Test
    public void shouldDeployDefaultRootBundleWithConfiguredRootBundleClassifier() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), hostName())
            .classifier("my.classifier")
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");
        givenConfiguredRootBundle("classifier", "my.classifier");

        boundary.apply(post, ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed(boundary.audits);
    }

    @Test
    public void shouldDeployDefaultRootBundleWithConfiguredRootBundleVersion() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");
        givenConfiguredRootBundle("version", "1.2");

        boundary.apply(post, emptyMap());

        jolokia.verifyDeployed(boundary.audits);
    }

    @Test
    public void shouldFailToDeployDefaultRootBundleWithoutConfiguredRootBundle() {
        givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");

        Throwable thrown = catchThrowable(() -> boundary.apply(post, emptyMap()));

        assertThat(thrown).hasMessageContaining("unresolved variable expression: root-bundle:version or version");
    }


    @Test
    public void shouldDeploySchemaBundleWithPassedParam() {
        givenConfiguredVariable("default.group-id", "artifact-deployer-test");
        LogHandlerFixture logHandler = givenLogHandler(periodicRotatingFile, "JOLOKIA");
        LoggerFixture logger = givenLogger("org.jolokia.jolokia")
            .level(DEBUG).handler("JOLOKIA").useParentHandlers(false);
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", hostName())
            .version("1")
            .containing(""
                + "log-handlers:\n"
                + "  ${toUpperCase(name)}:\n"
                + "    file: ${name}.log\n"
                + "loggers:\n"
                + "  ${group-id or default.group-id}.${name}:\n"
                + "    level: ${log-level or default.log-level or «DEBUG»}\n"
                + "    handlers:\n"
                + "    - ${toUpperCase(name)}\n"
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: ${group-id or default.group-id}\n"
                + "    artifact-id: ${artifact-id or name}\n"
                + "    version: ${version}\n");

        rootBundle.write(""
            + "bundles:\n"
            + "  ${hostName()}:\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n"
            + "        group-id: org.jolokia\n"
            + "        artifact-id: jolokia-war\n"
            + "        version: 1.3.3\n");
        boundary.apply(post, ImmutableMap.of(new VariableName("version"), "1"));

        logHandler.verifyAdded(boundary.audits);
        logger.verifyAdded(boundary.audits);
        jolokia.verifyDeployed(boundary.audits);
    }
}
