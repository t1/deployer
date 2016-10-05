package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import com.github.t1.testtools.LoggerMemento;
import io.dropwizard.testing.junit.DropwizardClientRule;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.net.URI;

import static com.github.t1.deployer.DeployerIT.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.UNKNOWN_CHECKSUM;
import static com.github.t1.log.LogLevel.*;
import static com.github.t1.rest.RestContext.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

public class ArtifactoryRepositoryIT {
    private static boolean TEST_WITH_REAL_ARTIFACTORY = false;
    private static ArtifactoryMock ARTIFACTORY_MOCK = TEST_WITH_REAL_ARTIFACTORY ? null : new ArtifactoryMock();

    @ClassRule
    public static DropwizardClientRule ARTIFACTORY = new DropwizardClientRule(
            TEST_WITH_REAL_ARTIFACTORY ? new Object[0] : new Object[] { ARTIFACTORY_MOCK });

    private URI baseUri = TEST_WITH_REAL_ARTIFACTORY
            ? URI.create("http://localhost:8081/artifactory")
            : ARTIFACTORY.baseUri();

    private RestContext config = REST.register("repository", baseUri);
    private final ArtifactoryRepository repository = new ArtifactoryRepository(config, "snapshots", "releases");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public LoggerMemento loggerMemento = new LoggerMemento()
            // .with("org.apache.http.wire", DEBUG)
            .with("org.apache.http.headers", DEBUG)
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

        assertThat(throwable)
                .isInstanceOf(UnknownChecksumException.class)
                .hasMessageContaining("unknown checksum: '" + UNKNOWN_CHECKSUM + "'");
    }

    @Test
    public void shouldSearchByChecksum() {
        Artifact artifact = repository.searchByChecksum(fakeChecksumFor(FOO));

        assertThat(artifact.getGroupId().getValue()).isEqualTo("org.foo");
        assertThat(artifact.getArtifactId().getValue()).isEqualTo("foo-war");
        assertThat(artifact.getVersion()).isEqualTo(CURRENT_FOO_VERSION);
        assertThat(artifact.getChecksum()).isEqualTo(fakeChecksumFor(FOO));
        assertThat(artifact.getType()).isEqualTo(war);
    }

    @Test
    public void shouldSearchByChecksumWithAuthorization() {
        assumeNotNull(ARTIFACTORY_MOCK);
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
    public void shouldFetchReleasedArtifact() throws Exception {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");
        Version version = new Version("1.3.3");

        Artifact artifact = repository.lookupArtifact(groupId, artifactId, version, null, war);

        assertThat(artifact.getGroupId()).isEqualTo(groupId);
        assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
        assertThat(artifact.getVersion()).isEqualTo(version);
        assertThat(artifact.getChecksum()).isEqualTo(JOLOKIA_1_3_3_CHECKSUM);
    }

    @Test
    public void shouldFetchSnapshotArtifact() throws Exception {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");
        Version version = new Version("1.3.4-SNAPSHOT");

        Artifact artifact = repository.lookupArtifact(groupId, artifactId, version, null, war);

        assertThat(artifact.getGroupId()).isEqualTo(groupId);
        assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
        assertThat(artifact.getVersion()).isEqualTo(version);
        assertThat(artifact.getChecksum()).isEqualTo(JOLOKIA_1_3_4_SNAPSHOT_CHECKSUM);
    }
}
