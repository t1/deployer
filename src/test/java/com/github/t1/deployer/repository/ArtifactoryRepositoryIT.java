package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.Version;
import com.github.t1.rest.Credentials;
import com.github.t1.rest.RestContext;
import com.github.t1.testtools.LoggerMemento;
import com.github.t1.testtools.TestLoggerRule;
import io.dropwizard.testing.junit.DropwizardClientRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;

import java.net.URI;
import java.util.List;

import static com.github.t1.deployer.model.ArtifactType.war;
import static com.github.t1.deployer.repository.ArtifactoryMock.AMBIGUOUS_CHECKSUM;
import static com.github.t1.deployer.repository.ArtifactoryMock.CURRENT_FOO_VERSION;
import static com.github.t1.deployer.repository.ArtifactoryMock.FAILING_CHECKSUM;
import static com.github.t1.deployer.repository.ArtifactoryMock.FOO;
import static com.github.t1.deployer.repository.ArtifactoryMock.UNKNOWN_CHECKSUM;
import static com.github.t1.deployer.repository.ArtifactoryMock.fakeChecksumFor;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_133_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_134_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_134_SNAPSHOT_CHECKSUM;
import static com.github.t1.log.LogLevel.DEBUG;
import static com.github.t1.rest.RestContext.REST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assume.assumeNotNull;

public class ArtifactoryRepositoryIT {
    private static final boolean TEST_WITH_REAL_ARTIFACTORY = false;
    private static final String SNAPSHOTS = TEST_WITH_REAL_ARTIFACTORY ? "snapshots-virtual" : "snapshots";
    private static final String RELEASES = TEST_WITH_REAL_ARTIFACTORY ? "releases-virtual" : "releases";
    private static final ArtifactoryMock ARTIFACTORY_MOCK = TEST_WITH_REAL_ARTIFACTORY ? null : new ArtifactoryMock();

    @ClassRule
    public static TestRule ARTIFACTORY = TEST_WITH_REAL_ARTIFACTORY
            ? new ExternalResource() {}
            : new DropwizardClientRule(ARTIFACTORY_MOCK);

    private URI baseUri = TEST_WITH_REAL_ARTIFACTORY
            ? URI.create("http://localhost:8081/artifactory")
            : ((DropwizardClientRule) ARTIFACTORY).baseUri();

    private RestContext rest = REST.register("repository", baseUri);
    private final ArtifactoryRepository repository = new ArtifactoryRepository(rest, SNAPSHOTS, RELEASES);

    @Rule public ExpectedException expectedException = ExpectedException.none();
    @Rule public TestLoggerRule logger = new TestLoggerRule();
    @Rule public LoggerMemento loggerMemento = new LoggerMemento()
            .with("org.apache.http.wire", DEBUG)
            // .with("org.apache.http.headers", DEBUG)
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
            rest = rest.register(baseUri, new Credentials("foo", "bar"));
            ARTIFACTORY_MOCK.setRequireAuthorization(true);

            Artifact artifact = new ArtifactoryRepository(rest, "snapshots", "releases")
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

        Artifact artifact = repository.resolveArtifact(groupId, artifactId, version, war, null);

        assertThat(artifact.getGroupId()).isEqualTo(groupId);
        assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
        assertThat(artifact.getVersion()).isEqualTo(version);
        assertThat(artifact.getChecksum()).isEqualTo(JOLOKIA_133_CHECKSUM);
    }

    @Test
    public void shouldFetchSnapshotArtifact() throws Exception {
        JOLOKIA_134_SNAPSHOT_CHECKSUM.hexByteArray(); // init
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");
        Version version = new Version("1.3.4-SNAPSHOT");

        Artifact artifact = repository.resolveArtifact(groupId, artifactId, version, war, null);

        assertThat(artifact.getGroupId()).isEqualTo(groupId);
        assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
        assertThat(artifact.getVersion()).isEqualTo(version);
        assertThat(artifact.getChecksum()).isEqualTo(JOLOKIA_134_SNAPSHOT_CHECKSUM);
    }

    @Test
    public void shouldFetchStableVersions() throws Exception {
        assert JOLOKIA_134_CHECKSUM != null; // make sure it's indexed
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");

        List<Version> versions = repository.listVersions(groupId, artifactId, false);

        assertThat(versions).extracting(Version::toString).contains("1.3.2", "1.3.3", "1.3.4");
        assertThat(versions).extracting(Version::toString).doesNotContain("1.3.4-SNAPSHOT");
    }

    @Test
    public void shouldFetchUnstableVersions() throws Exception {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");

        List<Version> versions = repository.listVersions(groupId, artifactId, true);

        assertThat(versions).extracting(Version::toString).contains("1.3.4-SNAPSHOT");
        assertThat(versions).extracting(Version::toString).doesNotContain("1.3.4");
    }
}
