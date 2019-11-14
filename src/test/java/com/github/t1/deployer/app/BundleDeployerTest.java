package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTests.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.Expressions.UnresolvedVariableException;
import com.github.t1.deployer.tools.CipherService;
import com.github.t1.deployer.tools.KeyStoreConfig;
import com.github.t1.problem.WebApplicationApplicationException;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;

import java.net.InetAddress;

import static com.github.t1.deployer.model.ArtifactType.bundle;
import static com.github.t1.deployer.model.ArtifactType.ear;
import static com.github.t1.deployer.model.ArtifactType.jar;
import static com.github.t1.deployer.model.Expressions.domainName;
import static com.github.t1.deployer.model.Expressions.hostName;
import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.deployer.testtools.TestData.VERSION;
import static com.github.t1.log.LogLevel.DEBUG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.quality.Strictness.LENIENT;

@MockitoSettings(strictness = LENIENT)
class BundleDeployerTest extends AbstractDeployerTests {
    private static final KeyStoreConfig KEYSTORE = new KeyStoreConfig()
        .setPath("src/test/resources/test.keystore")
        .setType("jceks")
        .setPass("changeit");

    private final CipherService cipher = new CipherService();

    private String encrypt(String plain) { return cipher.encrypt(plain, boundary.keyStore); }

    private static final Checksum UNKNOWN_CHECKSUM = Checksum.ofHexString("9999999999999999999999999999999999999999");

    @Test void shouldFailToDeployBundleAsDeployable() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  foo-war:\n"
            + "    type: bundle\n"
            + "    version: 1.0\n"
            + "    group-id: org.foo\n"));

        assertThat(thrown)
            .hasStackTraceContaining("a deployable may not be of type 'bundle'; use 'bundles' plan instead.");
    }


    @Test void shouldDeployWebArchiveWithOtherName() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();
        ArtifactFixture bar = givenArtifact("bar", foo.groupId(), foo.artifactId()).version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  bar:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo\n"
            + "    version: 1.3.2\n"
        );

        bar.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithVariables() {
        givenConfiguredVariable("fooGroupId", "org.foo");
        givenConfiguredVariable("fooArtifactId", "foo");
        givenConfiguredVariable("fooVersion", "1.3.2");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  ${fooArtifactId}:\n"
            + "    group-id: ${fooGroupId}\n"
            + "    version: ${fooVersion}\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithFirstOfTwoOrVariables() {
        givenConfiguredVariable("fooVersion", "1.3.2");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${fooVersion or barVersion}\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithSecondOfTwoOrVariables() {
        givenConfiguredVariable("barVersion", "1.3.2");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${fooVersion or barVersion}\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithEverythingDefault() {
        givenConfiguredVariable("default.group-id", "org.foo");
        givenConfiguredVariable("foo.version", "1.3.2");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithFirstOfThreeAlternativeVariables() {
        givenConfiguredVariable("fooName", "FOO");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  ${toLowerCase(fooName) or toLowerCase(barName) or toLowerCase(bazName)}:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithSecondOfThreeAlternativeVariables() {
        givenConfiguredVariable("barName", "FOO");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  ${toLowerCase(fooName) or toLowerCase(barName) or toLowerCase(bazName)}:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithThirdOfThreeAlternativeVariables() {
        givenConfiguredVariable("bazName", "FOO");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  ${toLowerCase(fooName) or toLowerCase(barName) or toLowerCase(bazName)}:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithHostNameFunction() throws Exception {
        String hostName = InetAddress.getLocalHost().getHostName().split("\\.")[0];
        ArtifactFixture foo = givenArtifact("foo").groupId(hostName).version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${hostName()}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test void shouldFailToResolveHostNameFunctionWithParameter() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${hostName(os.name)}\n"
            + "    version: 1.3.2\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("undefined function [hostName] with 1 params");
    }


    @Test void shouldDeployWebArchiveWithDomainNameFunction() throws Exception {
        String domainName = InetAddress.getLocalHost().getHostName().split("\\.", 2)[1];
        ArtifactFixture foo = givenArtifact("foo").groupId(domainName).version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${domainName()}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }

    @Test void shouldFailToResolveDomainNameFunctionWithParameter() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${domainName(os.name)}\n"
            + "    version: 1.3.2\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("undefined function [domainName] with 1 params");
    }


    @Test void shouldDeployWebArchiveWithStringLiteral() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${«org.foo»}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithStringLiteralContainingSingleQuotes() {
        ArtifactFixture foo = givenArtifact("foo").groupId("foo'bar'baz").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${«foo'bar'baz»}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithStringLiteralContainingDoubleQuotes() {
        ArtifactFixture foo = givenArtifact("foo").groupId("foo\"bar\"baz").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${«foo\"bar\"baz»}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithStringLiteralContainingGuillemetQuotes() {
        ArtifactFixture foo = givenArtifact("foo").groupId("foo«bar»baz").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${«foo«bar»baz»}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithOrParam() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${undefined0 or toLowerCase(undefined1 or «org.FOO»)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithRegexSuffix() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${regex(«org.foo01», «(.*?)\\d*»)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithRegexPrefix() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${regex(«qa.org.foo», «(?:qa\\.)(.*?)»)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithRegexVariable() {
        givenConfiguredVariable("my-regex", "(?:qa\\.)(.*?)");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${regex(«qa.org.foo», my-regex)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithPublicKeyEncryptedVersion() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(KEYSTORE.withAlias("keypair"));

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithPublicKeyEncryptedVersionUsingAliasParameter() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(KEYSTORE);

        String secret = cipher.encrypt(foo.getVersion().getValue(), boundary.keyStore.withAlias("keypair"));
        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + secret + "», «keypair»)}\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithSecretKeyEncryptedVersion() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(KEYSTORE.withAlias("secretkey"));

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithEncryptedVersionUsingDefaultKeystoreType() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(new KeyStoreConfig()
            .setPath("src/test/resources/jks.keystore")
            .setPass("changeit")
            .setAlias("keypair"));

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n");

        foo.verifyDeployed();
    }

    @Test void shouldFailToDeployWebArchiveWithEncryptedVariableButWithoutKeystoreConfig() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n"));

        assertThat(thrown).hasMessageContaining("no key-store configured");
    }

    @Test void shouldFailToDeployWebArchiveWithEncryptedVariableButWithoutPath() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(new KeyStoreConfig().setType("jceks").setPass("changeit").withAlias("keypair"));

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${decrypt(«" + encrypt(foo.getVersion().getValue()) + "»)}\n"));

        assertThat(thrown).hasMessageContaining("no key-store path configured");
    }


    @Test void shouldDeployWebArchiveWithFirstSwitch() {
        givenConfiguredVariable("bar", "A");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.1");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch(bar)\n"
            + "    A: «1.3.1»\n"
            + "    B: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithMiddleSwitch() {
        givenConfiguredVariable("bar", "B");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch(bar)\n"
            + "    A: «1.3.1»\n"
            + "    B: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithLastSwitch() {
        givenConfiguredVariable("bar", "C");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.3");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${switch(bar)\n"
            + "    A: «1.3.1»\n"
            + "    B: «1.3.2»\n"
            + "    C: «1.3.3»\n"
            + "    }\"\n");

        foo.verifyDeployed();
    }

    @Test void shouldFailToDeployWebArchiveWithSwitchWithoutHead() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
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

    @Test void shouldFailToDeployWebArchiveWithSwitchWithUnsetVariable() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
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

    @Test void shouldFailToDeployWebArchiveWithSwitchWithoutMatchingCase() {
        givenConfiguredVariable("bar", "B");

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
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

    @Test void shouldFailToDeployWebArchiveWithSwitchWithoutMatchingCaseButSomethingWithAPrefix() {
        givenConfiguredVariable("bar", "B");

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
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


    @Test void shouldDeployWebArchiveWithEncryptedVersionUsingSwitch() {
        givenConfiguredVariable("stage", "QA");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        givenConfiguredKeyStore(new KeyStoreConfig()
            .setPath("src/test/resources/jks.keystore")
            .setPass("changeit")
            .setAlias("keypair"));

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: \"${decrypt(switch(stage)\n"
            + "      TEST: «test-dummy»\n"
            + "      QA: «" + encrypt(foo.getVersion().getValue()) + "»\n"
            + "      PROD: «prod-dummy»\n"
            + "      )}\"\n");

        foo.verifyDeployed();
    }


    @Test void shouldFailToReplaceVariableValueWithNewline() {
        shouldFailToReplaceVariableValueWith("foo\nbar");
    }

    @Test void shouldFailToReplaceVariableValueWithTab() { shouldFailToReplaceVariableValueWith("\tfoo"); }

    private void shouldFailToReplaceVariableValueWith(String value) {
        givenConfiguredVariable("foo", value);

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  ${foo}:\n"
            + "    group-id: org.foo\n"
            + "    version: 1\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("invalid character in variable value for [foo]");
    }


    @Test void shouldDeployWebArchiveWithTwoVariablesInOneLine() {
        givenConfiguredVariable("orgVar", "org");
        givenConfiguredVariable("fooVar", "foo");
        ArtifactFixture foo = givenArtifact("foo").version("1");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${orgVar}.${fooVar}\n"
            + "    version: 1\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithCommentAfterVariable() {
        givenConfiguredVariable("orgVar", "org");
        ArtifactFixture foo = givenArtifact("foo").version("1");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${orgVar}.foo # cool\n"
            + "    version: 1\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithDollarString() {
        ArtifactFixture foo = givenArtifact("foo").version("$1");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: $1\n");

        foo.verifyDeployed();
    }


    @Test void shouldDeployWebArchiveWithVariableInComment() {
        ArtifactFixture foo = givenArtifact("foo").version("1");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1 # ${not} cool\n"
            + "# absolutely ${not} cool");

        foo.verifyDeployed();
    }


    @Test void shouldFailToDeployWebArchiveWithUndefinedVariable() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: ${undefined}\n"
            + "    version: 1.2\n"));

        assertThat(thrown)
            .hasRootCauseExactlyInstanceOf(UnresolvedVariableException.class)
            .hasMessageContaining("unresolved variable expression: undefined");
    }


    @Test void shouldDeployWebArchiveWithEscapedVariable() {
        ArtifactFixture foo = givenArtifact("${bar}", "org.foo", "foo-war").version("1");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  $${bar}:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo-war\n"
            + "    version: 1\n");

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithChangedCaseVariables() {
        givenConfiguredVariable("foo", "foo");
        ArtifactFixture foo = givenArtifact("FOO", "org.foo", "Foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  ${toUpperCase(foo)}:\n"
            + "    group-id: org.${toLowerCase(foo)}\n"
            + "    artifact-id: ${toInitCap(foo)}\n"
            + "    version: 1.3.2\n");

        foo.verifyDeployed();
    }


    @Test void shouldFailToDeployWebArchiveWithUndefinedVariableFunction() {
        givenConfiguredVariable("foo", "Foo");

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  ${bar(foo)}:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo\n"
            + "    version: 1.3.2\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("undefined function [bar] with 1 params");
    }

    @Test void shouldFailToDeployWebArchiveWithVariableFunctionMissingParam() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  ${toLowerCase()}:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo\n"
            + "    version: 1.3.2\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("undefined function [toLowerCase] with 0 params");
    }

    @Test void shouldFailToDeployWebArchiveWithUndefinedFunctionVariable() {
        Throwable thrown = catchThrowable(() ->
            deployWithRootBundle(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: ${toLowerCase(undefined)}\n"
                + "    version: 1.2\n"));

        assertThat(thrown)
            .hasRootCauseExactlyInstanceOf(UnresolvedVariableException.class)
            .hasMessageContaining("unresolved variable expression: toLowerCase(undefined)");
    }


    @Test void shouldUpdateExistingWebArchive() {
        ArtifactFixture foo2 = givenArtifact("foo")
            .version("1").deployed()
            .and()
            .version("2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 2\n");

        foo2.verifyRedeployed();
    }


    @Test void shouldNotRedeployWebArchiveWithSameNameAndChecksum() {
        ArtifactFixture foo = givenArtifact("foo")
            .version("1").deployed()
            .and()
            .version("2");

        deployWithRootBundle(""
            + "org.foo:\n"
            + "  foo-war:\n"
            + "    version: 1\n");

        foo.verifyUnchanged();
    }


    @Test void shouldDeployWebArchiveWithSameChecksumButDifferentName() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar", "org.foo", "foo-war").version("1").checksum(foo.getChecksum());

        deployWithRootBundle(""
            + "deployables:\n"
            + "  bar:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo-war\n"
            + "    version: 1\n"
        );

        foo.verifyUnchanged();
        bar.verifyDeployed();
    }


    @Test void shouldNotDeployWebArchiveWithSameNameButDifferentGroup() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar", foo.groupId(), foo.artifactId()).version("1");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  bar:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo\n"
            + "    version: 1\n"
        );

        foo.verifyUnchanged();
        bar.verifyDeployed();
    }


    @Test void shouldDeploySecondWebArchive() {
        ArtifactFixture jolokia = givenArtifact("jolokia")
            .version("1.3.2").deployed()
            .and()
            .version("1.3.3");
        ArtifactFixture mockServer = givenArtifact("mockserver", "org.mock-server", "mockserver-war")
            .version("3.10.4");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    group-id: org.jolokia\n"
            + "    version: 1.3.2\n"
            + "  mockserver:\n"
            + "    group-id: org.mock-server\n"
            + "    artifact-id: mockserver-war\n"
            + "    version: 3.10.4\n");

        jolokia.verifyUnchanged();
        mockServer.verifyDeployed();
    }


    @Test void shouldUndeployWebArchiveWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n"
            + "    state: undeployed\n"
        );

        foo.verifyRemoved();
    }


    @Test void shouldUndeployWebArchiveWithoutVersionAndGroupId() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    state: undeployed\n");

        foo.verifyRemoved();
    }


    @Test void shouldUndeployWebArchiveWithOtherVersionWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 999\n"
            + "    state: undeployed\n");

        foo.verifyRemoved();
    }

    @Test void shouldUndeployWebArchiveWithOtherGroupIdWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.bar\n"
            + "    version: 1.3.2\n"
            + "    state: undeployed\n");

        foo.verifyRemoved();
    }

    @Test void shouldUndeployWebArchiveWithOtherArtifactIdWhenStateIsUndeployed() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: bar\n"
            + "    version: 1.3.2\n"
            + "    state: undeployed\n");

        foo.verifyRemoved();
    }

    @Test void shouldFailToUndeployWebArchiveWithWrongChecksum() {
        givenArtifact("foo").version("1.3.2").deployed();

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
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


    @Test void shouldRenameWebArchiveWithSameChecksumWhenManaged() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar", "org.foo", "foo-war").version("1");
        givenManaged("deployables");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  bar:\n"
            + "    group-id: org.foo\n"
            + "    artifact-id: foo-war\n"
            + "    version: 1\n"
        );

        bar.verifyDeployed();
        foo.verifyRemoved();
    }


    @Test void shouldUndeployUnspecifiedWebArchiveWhenManaged() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        givenManaged("deployables");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    group-id: org.jolokia\n"
            + "    version: 1.3.2\n");

        jolokia.verifyUnchanged();
        mockserver.verifyRemoved();
    }


    @Test void shouldUndeployUnspecifiedWebArchiveWhenAllManaged() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        givenManaged("all");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    group-id: org.jolokia\n"
            + "    version: 1.3.2\n");

        jolokia.verifyUnchanged();
        mockserver.verifyRemoved();
    }


    @Test void shouldDeployWebArchiveWithConfiguredVariable() {
        givenConfiguredVariable("v", "1.3.3");
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    group-id: org.jolokia\n"
            + "    artifact-id: jolokia-war\n"
            + "    version: ${v}");

        jolokia.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithDefaultGroupId() {
        givenConfiguredVariable("default.group-id", "org.jolokia");
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    artifact-id: jolokia-war\n"
            + "    version: 1.3.3\n");

        jolokia.verifyDeployed();
    }


    @Test void shouldDeployJar() {
        ArtifactFixture postgresql = givenArtifact(jar, "postgresql").version("9.4.1207");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  postgresql:\n"
            + "    group-id: org.postgresql\n"
            + "    version: 9.4.1207\n"
            + "    type: jar\n");

        postgresql.verifyDeployed();
    }


    @Test void shouldDeployJarWithDefaultType() {
        givenConfiguredVariable("default.deployable-type", "jar");
        ArtifactFixture postgresql = givenArtifact(jar, "postgresql").version("9.4.1207");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  postgresql:\n"
            + "    group-id: org.postgresql\n"
            + "    version: 9.4.1207\n");

        postgresql.verifyDeployed();
    }


    @Test void shouldDeployEar() {
        ArtifactFixture postgresql = givenArtifact(ear, "foo").version("1");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1\n"
            + "    type: ear\n");

        postgresql.verifyDeployed();
    }


    @Test void shouldUndeployEverythingWhenManagedAndEmptyPlan() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2").deployed();
        ArtifactFixture mockserver = givenArtifact("org.mock-server", "mockserver-war").version("3.10.4").deployed();
        givenManaged("deployables", "loggers", "log-handlers");

        deployWithRootBundle("---\n");

        jolokia.verifyRemoved();
        mockserver.verifyRemoved();
    }

    @Test void shouldDeployLatestWebArchive() {
        ArtifactFixture latest = givenArtifact("foo")
            .version("1.3.2").and()
            .version("1.4.4").and()
            .version("2.0.0-SNAPSHOT").and()
            .version("1.5.0-SNAPSHOT").and()
            .version("1.4.11");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: LATEST\n"
        );

        latest.verifyDeployed();
    }

    @Test void shouldDeployUnstableWebArchive() {
        ArtifactFixture unstable = givenArtifact("foo")
            .version("1.3.2").and()
            .version("1.4.4").and()
            .version("1.4.11").and()
            .version("1.5.0-SNAPSHOT").and()
            .version("2.0.0-SNAPSHOT");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: UNSTABLE\n"
        );

        unstable.verifyDeployed();
    }

    @Test void shouldDeployUnstableButReleasedWebArchive() {
        ArtifactFixture unstable = givenArtifact("foo")
            .version("1.3.2").and()
            .version("1.4.4").and()
            .version("1.4.11").and()
            .version("1.5.0-SNAPSHOT").and()
            .version("2.0.0-SNAPSHOT").and()
            .version("2.0.0");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: UNSTABLE\n"
        );

        unstable.verifyDeployed();
    }

    @Test void shouldDeployBundle() {
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.2").deployed();
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

        deployWithRootBundle(""
            + "bundles:\n"
            + "  some-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n");

        jolokia.verifyUnchanged();
        mockserver.verifyDeployed();
    }


    @Test void shouldDeployBundleWithSystemParam() {
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

        deployWithRootBundle(""
            + "bundles:\n"
            + "  bundle-with-system-param:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n");

        jolokia.verifyDeployed();
    }

    @Test void shouldDeployBundleWithPassedParam() {
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "bundle-with-passed-param")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: ${v}\n");

        deployWithRootBundle(""
            + "bundles:\n"
            + "  bundle-with-passed-param:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n"
            + "        v: 1.3.3\n");

        jolokia.verifyDeployed();
    }


    @Test void shouldDeployBundleWithName() {
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "bundle-with-name")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.3\n");

        deployWithRootBundle(""
            + "bundles:\n"
            + "  bundle-with-name:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n");

        jolokia.verifyDeployed();
    }


    @Test void shouldDeployLatestBundle() {
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

        deployWithRootBundle(""
            + "bundles:\n"
            + "  latest-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n");

        latest.verifyDeployed();
    }


    @Test void shouldDeployUnstableBundle() {
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

        deployWithRootBundle(""
            + "bundles:\n"
            + "  unstable-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n");

        unstable.verifyDeployed();
    }


    @Test void shouldDeployBundleWithDefaultVersionExpression() {
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

        deployWithRootBundle(""
            + "bundles:\n"
            + "  expression-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n"
            + "        version: ${jolokia.version}\n");

        jolokia.verifyDeployed();
    }


    @Test void shouldNotDeployBundleWithDefaultCurrentVersionExpression() {
        ArtifactFixture jolokia = givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.2").deployed();
        ArtifactFixture foo = givenArtifact(bundle, "artifact-deployer-test", "current-expression-bundle")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: ${version}\n");

        deployWithRootBundle(""
            + "bundles:\n"
            + "  current-expression-bundle:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"
            + "    instances:\n"
            + "      jolokia:\n"
            + "        version: ${jolokia.version}\n");

        jolokia.verifyUnchanged();
        foo.verifyUnchanged();
    }


    @Test void shouldFailToDeployBundleWithoutName() {
        givenArtifact("jolokia", "org.jolokia", "jolokia-war").version("1.3.3");
        givenArtifact(bundle, "artifact-deployer-test", "bundle-without-name")
            .version("1")
            .containing(""
                + "deployables:\n"
                + "  ${name}:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.3\n");

        Throwable throwable = catchThrowable(() -> deployWithRootBundle(""
            + "bundles:\n"
            + "  bundle-without-name:\n"
            + "    group-id: artifact-deployer-test\n"
            + "    version: 1\n"));

        assertThat(throwable)
            .hasRootCauseExactlyInstanceOf(UnresolvedVariableException.class)
            .hasMessageContaining("name");
    }


    @Test void shouldFailToDeployDefaultRootBundleWithoutConfigFile() {
        boundary.useDefaultConfig = true;

        Throwable thrown = catchThrowable(() -> postVariables(ImmutableMap.of(VERSION, "1.2")));

        assertThat(thrown).hasMessageContaining(
            "applying the default root bundle is only allowed when there is a configuration file");
    }

    @Test void shouldDeployDefaultRootBundle() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        ArtifactFixture dummy = givenArtifact(bundle, "dummy", domainName(), hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");

        postVariables(ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed();
        dummy.verifyUnchanged();
    }

    @Test void shouldDeployDefaultRootBundleWithDefaultGroupId() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenConfiguredVariable("default.group-id", "mygroup");
        givenArtifact(bundle, "dummy", "mygroup", hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");

        postVariables(ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed();
    }

    @Test void shouldDeployDefaultRootBundleWithConfiguredRootBundleArtifactId() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), "foo")
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");
        givenConfiguredRootBundle("artifact-id", "foo");

        postVariables(ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed();
    }

    @Test void shouldDeployDefaultRootBundleWithConfiguredRootBundleArtifactIdExpression() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");
        givenConfiguredRootBundle("artifact-id", "${hostName()}");

        postVariables(ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed();
    }

    @Test void shouldDeployDefaultRootBundleWithConfiguredRootBundleGroupId() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", "my.other.group", hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");
        givenConfiguredRootBundle("group-id", "my.other.group");

        postVariables(ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed();
    }

    @Test void shouldDeployDefaultRootBundleWithConfiguredRootBundleClassifier() {
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

        postVariables(ImmutableMap.of(VERSION, "1.2"));

        jolokia.verifyDeployed();
    }

    @Test void shouldDeployDefaultRootBundleWithConfiguredRootBundleVersion() {
        ArtifactFixture jolokia = givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");
        givenConfiguredRootBundle("version", "1.2");

        post();

        jolokia.verifyDeployed();
    }

    @Test void shouldFailToDeployDefaultRootBundleWithoutConfiguredRootBundle() {
        givenArtifact("jolokia").version("1.3.2");
        givenArtifact(bundle, "dummy", domainName(), hostName())
            .version("1.2")
            .containing(""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 1.3.2\n");

        Throwable thrown = catchThrowable(this::post);

        assertThat(thrown).hasMessageContaining("unresolved variable expression: root-bundle:version or version");
    }


    @Test void shouldDeploySchemaBundleWithPassedParam() {
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
        postVariables(ImmutableMap.of(VERSION, "1"));

        logHandler.verifyAdded();
        logger.verifyAdded();
        jolokia.verifyDeployed();
    }
}
