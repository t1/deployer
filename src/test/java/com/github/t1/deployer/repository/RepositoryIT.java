package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import com.github.t1.testtools.LoggerMemento;
import io.dropwizard.testing.junit.DropwizardClientRule;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.net.URI;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static com.github.t1.log.LogLevel.*;
import static com.github.t1.rest.RestContext.*;
import static org.assertj.core.api.Assertions.*;

public class RepositoryIT {
    private static ArtifactoryMock ARTIFACTORY_MOCK = new ArtifactoryMock();

    @ClassRule
    public static DropwizardClientRule ARTIFACTORY = new DropwizardClientRule(ARTIFACTORY_MOCK);
    private final URI baseUri = URI.create(ARTIFACTORY.baseUri() + "/artifactory");
    private RestContext config = REST.register("repository", baseUri);
    private final ArtifactoryRepository repository = new ArtifactoryRepository(config, "snapshots", "releases");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public LoggerMemento loggerMemento = new LoggerMemento()
            .with("org.apache.http.wire", DEBUG)
            // .with("com.github.t1.rest", DEBUG)
            .with("com.github.t1.deployer", DEBUG);

    @BeforeClass public static void before() { ArtifactoryMock.FAKES = true; }

    @AfterClass public static void after() { ArtifactoryMock.FAKES = false; }


    @Test
    public void shouldFailToSearchByChecksumWhenUnavailable() {
        Throwable throwable = catchThrowable(() -> repository.searchByChecksum(FAILING_CHECKSUM));

        assertThat(throwable).hasMessageContaining("error while searching for checksum: '" + FAILING_CHECKSUM + "'");
    }

    @Test
    public void shouldFailToSearchByChecksumWhenAmbiguous() {
        Throwable throwable = catchThrowable(() -> repository.searchByChecksum(AMBIGUOUS_CHECKSUM));

        assertThat(throwable).hasMessageContaining("checksum not unique in repository: '" + AMBIGUOUS_CHECKSUM + "'");
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnknown() {
        Throwable throwable = catchThrowable(() -> repository.searchByChecksum(UNKNOWN_CHECKSUM));

        assertThat(throwable).hasMessageContaining("unknown checksum: '" + UNKNOWN_CHECKSUM + "'");
    }

    @Ignore
    @Test
    public void shouldSearchByChecksum() {
        Artifact artifact = repository.searchByChecksum(fakeChecksumFor(FOO));

        assertThat(artifact.getGroupId().getValue()).isEqualTo("org.foo");
        assertThat(artifact.getArtifactId().getValue()).isEqualTo("foo-war");
        assertThat(artifact.getVersion()).isEqualTo(CURRENT_FOO_VERSION);
        assertThat(artifact.getChecksum()).isEqualTo(fakeChecksumFor(FOO));
        assertThat(artifact.getType()).isEqualTo(war);
    }

    @Ignore
    @Test
    public void shouldSearchByChecksumWithAuthorization() {
        try {
            config = config.register(baseUri, new Credentials("foo", "bar"));
            ARTIFACTORY_MOCK.setRequireAuthorization(true);

            Artifact artifact = new ArtifactoryRepository(config, "snapshots", "releases")
                    .searchByChecksum(fakeChecksumFor(FOO));

            assertThat(artifact.getGroupId().getValue()).isEqualTo("org.foo");
            assertThat(artifact.getArtifactId().getValue()).isEqualTo("foo-war");
            assertThat(artifact.getVersion()).isEqualTo(CURRENT_FOO_VERSION);
            assertThat(artifact.getChecksum()).isEqualTo(fakeChecksumFor(FOO));
            assertThat(artifact.getType()).isEqualTo(war);
        } finally {
            ARTIFACTORY_MOCK.setRequireAuthorization(false);
        }
    }

    @Test
    public void shouldFetchSnapshotArtifact() throws Exception {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");
        Version version = new Version("1.3.3-SNAPSHOT");

        Artifact artifact = repository.lookupArtifact(groupId, artifactId, version, war);

        assertThat(artifact.getGroupId()).isEqualTo(groupId);
        assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
        assertThat(artifact.getVersion()).isEqualTo(version);
        assertThat(artifact.getChecksum()).isEqualTo(Checksum.fromString("FACE0000198C532FB516A3E79549519DA78A0655"));
    }

    @Test
    public void shouldFetchReleasedArtifact() throws Exception {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");
        Version version = new Version("1.3.3");

        Artifact artifact = repository.lookupArtifact(groupId, artifactId, version, war);

        assertThat(artifact.getGroupId()).isEqualTo(groupId);
        assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
        assertThat(artifact.getVersion()).isEqualTo(version);
        assertThat(artifact.getChecksum()).isEqualTo(Checksum.fromString("F6E5786754116CC8E1E9261B2A117701747B1259"));
    }
}
