package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTests.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.problem.WebApplicationApplicationException;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.quality.Strictness.LENIENT;

@MockitoSettings(strictness = LENIENT)
class ArtifactDeployerTest extends AbstractDeployerTests {
    @Test void shouldDeployWebArchive() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n"
        );

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveEvenWithInvalidSystemProperty() {
        systemProperties.given("foo:bar", "foobar");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n"
        );

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithClassifier() {
        ArtifactFixture foo = givenArtifact("foo").classifier("plus").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    classifier: plus\n"
            + "    version: 1.3.2\n"
        );

        foo.verifyDeployed();
    }

    @Test void shouldDeployWebArchiveWithCorrectChecksum() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n"
            + "    checksum: " + foo.getChecksum() + "\n"
        );

        foo.verifyDeployed();
    }

    @Test void shouldFailToDeployWebArchiveWithIncorrectChecksum() {
        givenArtifact("foo").version("1.3.2");

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
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

    @Test void shouldUpdateWebArchiveWithCorrectChecksum() {
        givenArtifact("foo").version("1.3.1").deployed();
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n"
            + "    checksum: " + foo.getChecksum() + "\n"
        );

        foo.verifyDeployed();
    }

    @Test void shouldFailToUpdateWebArchiveWithIncorrectChecksum() {
        givenArtifact("foo").version("1.3.1").deployed();
        givenArtifact("foo").version("1.3.2");

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
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

    @Test void shouldFailToCheckWebArchiveWithIncorrectChecksum() {
        givenArtifact("foo").version("1.3.1").deployed();

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
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

    @Test void shouldDeployEmptyDeployables() {
        deployWithRootBundle(""
            + "deployables:\n");

        assertThat(boundary.audits.getAudits()).isEmpty();
    }


    @Test void shouldFailToDeployWebArchiveWithEmptyItem() {
        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  foo-war:\n"));

        assertThat(thrown).hasStackTraceContaining("incomplete deployables plan 'foo-war'");
    }


    @Test void shouldFailToDeployWebArchiveWithoutGroupId() {
        givenArtifact("foo").version("1.3.2");

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    state: deployed\n"
            + "    version: 1.3.2\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("the `group-id` can only be null when undeploying");
    }


    @Test void shouldSkipUndeployedWebArchiveWithVersionVariableDefaultingToCurrent() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${foo.version}\n"
            + "    state: ${foo.state or «deployed»}\n"
        );

        foo.verifySkipped();
    }

    @Test void shouldSkipUndeployedWebArchiveWithEmptyVersionVariable() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${foo.version}\n"
                + "    state: ${foo.state or «deployed»}\n",
            ImmutableMap.of(new VariableName("bar.version"), ""));

        foo.verifySkipped();
    }

    @Test void shouldSkipUndeployedWebArchiveWithNullVersionVariable() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        Map<VariableName, String> variables = new HashMap<>();
        variables.put(new VariableName("bar.version"), null);

        deployWithRootBundle(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${foo.version}\n"
                + "    state: ${foo.state or «deployed»}\n",
            variables);

        foo.verifySkipped();
    }

    @Test void shouldSkipDeployedWebArchiveWithVersionVariableDefaultingToCurrent() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: ${foo.version}\n"
            + "    state: ${foo.state or «deployed»}\n"
        );

        foo.verifyUnchanged();
    }

    @Test void shouldSkipUndeployedWebArchiveWithoutPlannedVersion() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    state: deployed\n");

        foo.verifySkipped();
    }


    @Test void shouldSkipDeployedWebArchiveWithPlannedVersionExplicitlyCurrent() {
        ArtifactFixture foo = givenUnknownArtifact("foo").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: CURRENT\n");

        foo.verifyUnchanged();
    }


    @Test void shouldNotDeployWebArchiveWithoutVersion() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n");

        foo.verifyUnchanged();
    }

    @Test void shouldNotDeployWebArchiveWithCurrentVersionVariable() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: CURRENT\n");

        foo.verifyUnchanged();
    }

    @Test void shouldDeploySecondWebArchiveWithOnlyOneVersionVariable() {
        givenArtifact("foo").version("1.3.2").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("4.0.5");

        deployWithRootBundle(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${foo.version}\n"
                + "  bar:\n"
                + "    group-id: org.bar\n"
                + "    version: ${bar.version}\n",
            ImmutableMap.of(new VariableName("bar.version"), "4.0.5"));

        bar.verifyDeployed();
    }


    @Test void shouldSkipOneOfTwoDeployedWebArchiveWithVersionDefaultingToCurrent() {
        ArtifactFixture foo = givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("2");

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "  bar:\n"
            + "    group-id: org.bar\n"
            + "    version: 2\n");

        bar.verifyDeployed();
        foo.verifyUnchanged();
    }

    @Test void shouldUndeployWebArchive() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n"
            + "    state: undeployed\n");

        foo.verifyRemoved();
    }

    @Test void shouldUndeployWebArchiveWhenManaged() {
        givenManaged("all");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        deployWithRootBundle(""
            + "deployables:\n");

        foo.verifyRemoved();
    }

    @Test void shouldIgnorePinnedWebArchiveWhenManaged() {
        givenManaged("all");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("2").deployed().pinned();
        ArtifactFixture baz = givenArtifact("baz").version("3").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n");

        foo.verifyUnchanged();
        bar.verifyUnchanged();
        baz.verifyRemoved();
    }


    @Test void shouldIgnoreDeployerWhenManaged() {
        givenManaged("all");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("2").deployed();
        ArtifactFixture deployer = givenArtifact("deployer").version("3").deployed();

        deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n");

        foo.verifyUnchanged();
        bar.verifyRemoved();
        deployer.verifyUnchanged();
    }

    @Test void shouldFailToDeployPinnedWebArchive() {
        givenArtifact("foo").version("1.3.2").deployed().pinned();

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "deployables:\n"
            + "  foo:\n"
            + "    group-id: org.foo\n"
            + "    version: 1.3.2\n"));

        assertThat(thrown)
            .isInstanceOf(WebApplicationApplicationException.class)
            .hasMessageContaining("resource is pinned: deployment:foo:deployed:org.foo:foo:1.3.2:war");
    }
}
