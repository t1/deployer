package com.github.t1.deployer.app;

import com.github.t1.deployer.app.AbstractDeployerTests.ArtifactFixtureBuilder.ArtifactFixture;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.problem.WebApplicationApplicationException;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.github.t1.deployer.app.Trigger.*;
import static org.assertj.core.api.Assertions.*;

public class ArtifactDeployerTest extends AbstractDeployerTests {
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
    public void shouldDeployWebArchiveEvenWithInvalidSystemProperty() {
        systemProperties.given("foo:bar", "foobar");
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
    public void shouldDeployWebArchiveWithClassifier() {
        ArtifactFixture foo = givenArtifact("foo").classifier("plus").version("1.3.2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    classifier: plus\n"
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

        assertThat(thrown).hasStackTraceContaining("incomplete deployables plan 'foo-war'");
    }


    @Test
    public void shouldFailToDeployWebArchiveWithoutGroupId() {
        givenArtifact("foo").version("1.3.2");

        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    state: deployed\n"
                + "    version: 1.3.2\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("the `group-id` can only be null when undeploying");
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
                .hasMessageContaining("artifact not found: deployment:foo:deployed:org.foo:foo:CURRENT:war");
    }


    @Test
    public void shouldNotDeployWebArchiveWithoutVersion() {
        givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n");

        assertThat(audits.getAudits()).isEmpty();
    }

    @Test
    public void shouldNotDeployWebArchiveWithCurrentVersionVariable() {
        givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: CURRENT\n");

        assertThat(audits.getAudits()).isEmpty();
    }

    @Test
    public void shouldDeploySecondWebArchiveWithOnlyOneVersionVariable() throws Exception {
        givenArtifact("foo").version("1.3.2").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("4.0.5");

        rootBundle.write(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: ${foo.version}\n"
                + "  bar:\n"
                + "    group-id: org.bar\n"
                + "    version: ${bar.version}\n");
        Audits audits = boundary.apply(mock, ImmutableMap.of(new VariableName("bar.version"), "4.0.5"));

        bar.verifyDeployed(audits);
    }


    @Test
    public void shouldSkipDeployedWebArchiveWithoutVersion() {
        givenArtifact("foo").version("1").deployed();
        ArtifactFixture bar = givenArtifact("bar").version("2");

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "  bar:\n"
                + "    group-id: org.bar\n"
                + "    version: 2\n");

        bar.verifyDeployed(audits);
    }

    @Test
    public void shouldUndeployWebArchive() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed\n");

        foo.verifyUndeployExecuted();
        assertThat(audits.getAudits()).containsExactly(foo.removedAudit());
    }

    @Test
    public void shouldUndeployWebArchiveWhenManaged() {
        givenManaged("all");
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2").deployed();

        Audits audits = deploy(""
                + "deployables:\n");

        foo.verifyUndeployExecuted();
        assertThat(audits.getAudits()).containsExactly(foo.removedAudit());
    }

    @Test
    public void shouldIgnorePinnedWebArchiveWhenManaged() {
        givenManaged("all");
        givenArtifact("foo").version("1.3.2").deployed();
        givenArtifact("bar").version("2").deployed().pinned();
        ArtifactFixture baz = givenArtifact("baz").version("3").deployed();

        Audits audits = deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n");

        baz.verifyRemoved(audits);
    }

    @Test
    public void shouldFailToDeployPinnedWebArchive() {
        givenArtifact("foo").version("1.3.2").deployed().pinned();

        Throwable thrown = catchThrowable(() -> deploy(""
                + "deployables:\n"
                + "  foo:\n"
                + "    group-id: org.foo\n"
                + "    version: 1.3.2\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("resource is pinned: deployment:foo:deployed:org.foo:foo:1.3.2:war");
    }
}
