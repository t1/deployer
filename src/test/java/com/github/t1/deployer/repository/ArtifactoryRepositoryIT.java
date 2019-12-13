package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.Version;
import com.github.t1.deployer.tools.Password;
import com.github.t1.deployer.tools.RepositoryAuthenticator;
import com.github.t1.jaxrsclienttest.JaxRsTestExtension;
import com.github.t1.testtools.LoggerMemento;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.util.List;

import static com.github.t1.deployer.model.ArtifactType.war;
import static com.github.t1.deployer.repository.ArtifactoryMock.AMBIGUOUS_CHECKSUM;
import static com.github.t1.deployer.repository.ArtifactoryMock.CURRENT_FOO_VERSION;
import static com.github.t1.deployer.repository.ArtifactoryMock.FAILING_CHECKSUM;
import static com.github.t1.deployer.repository.ArtifactoryMock.FOO;
import static com.github.t1.deployer.repository.ArtifactoryMock.UNKNOWN_CHECKSUM;
import static com.github.t1.deployer.repository.ArtifactoryMock.fakeChecksumFor;
import static com.github.t1.deployer.repository.RepositoryProducer.DEFAULT_ARTIFACTORY_URI;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_133_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_134_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_134_SNAPSHOT_CHECKSUM;
import static com.github.t1.log.LogLevel.DEBUG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

class ArtifactoryRepositoryIT {
    private static final boolean TEST_WITH_REAL_ARTIFACTORY = false;
    private static final String SNAPSHOTS = TEST_WITH_REAL_ARTIFACTORY ? "snapshots-virtual" : "snapshots";
    private static final String RELEASES = TEST_WITH_REAL_ARTIFACTORY ? "releases-virtual" : "releases";
    private static final ArtifactoryMock ARTIFACTORY_MOCK = TEST_WITH_REAL_ARTIFACTORY ? null : new ArtifactoryMock();

    @RegisterExtension LoggerMemento loggerMemento = new LoggerMemento()
        .with("org.apache.http.wire", DEBUG)
        // .with("org.apache.http.headers", DEBUG)
        .with("com.github.t1.deployer", DEBUG);

    @RegisterExtension static Extension ARTIFACTORY = TEST_WITH_REAL_ARTIFACTORY
        ? new Extension() {} // do nothing
        : new JaxRsTestExtension(ARTIFACTORY_MOCK);

    private URI baseUri = TEST_WITH_REAL_ARTIFACTORY
        ? DEFAULT_ARTIFACTORY_URI
        : ((JaxRsTestExtension) ARTIFACTORY).baseUri();

    private final RepositoryAuthenticator authenticator = new RepositoryAuthenticator();
    private final Client client = ClientBuilder.newClient().register(authenticator).register(XmlMessageBodyReader.class);

    private final ArtifactoryRepository repository = new ArtifactoryRepository(client, baseUri, SNAPSHOTS, RELEASES);

    @BeforeAll static void before() { ArtifactoryMock.FAKES = true; }

    @AfterAll static void after() { ArtifactoryMock.FAKES = false; }


    @Test void shouldFailToSearchByChecksumWhenUnavailable() {
        ErrorWhileFetchingChecksumException throwable = catchThrowableOfType(() -> repository.searchByChecksum(FAILING_CHECKSUM),
            ErrorWhileFetchingChecksumException.class);

        assertThat(throwable.checksum).isEqualTo(FAILING_CHECKSUM);
    }

    @Test void shouldFailToSearchByChecksumWhenAmbiguous() {
        Throwable throwable = catchThrowable(() -> repository.searchByChecksum(AMBIGUOUS_CHECKSUM));

        assertThat(throwable).hasMessageContaining("checksum not unique in repository: '" + AMBIGUOUS_CHECKSUM + "'");
    }

    @Test void shouldFailToSearchByChecksumWhenUnknown() {
        UnknownChecksumException throwable = catchThrowableOfType(() -> repository.searchByChecksum(UNKNOWN_CHECKSUM),
            UnknownChecksumException.class);

        assertThat(throwable.checksum).isEqualTo(UNKNOWN_CHECKSUM);
    }

    @Test void shouldSearchByChecksum() {
        Artifact artifact = repository.searchByChecksum(fakeChecksumFor(FOO));

        assertThat(artifact.getGroupId().getValue()).isEqualTo("org.foo");
        assertThat(artifact.getArtifactId().getValue()).isEqualTo("foo-war");
        assertThat(artifact.getVersion()).isEqualTo(CURRENT_FOO_VERSION);
        assertThat(artifact.getChecksum()).isEqualTo(fakeChecksumFor(FOO));
        assertThat(artifact.getType()).isEqualTo(war);
    }

    @Test void shouldSearchByChecksumWithAuthorization() {
        assumeThat(ARTIFACTORY_MOCK).isNotNull();
        try {
            ARTIFACTORY_MOCK.setRequireAuthorization(true);

            authenticator.setUri(baseUri);
            authenticator.setUsername("foo");
            authenticator.setPassword(new Password("bar"));

            Artifact artifact = repository.searchByChecksum(fakeChecksumFor(FOO));

            assertThat(artifact.getGroupId().getValue()).isEqualTo("org.foo");
            assertThat(artifact.getArtifactId().getValue()).isEqualTo("foo-war");
            assertThat(artifact.getVersion()).isEqualTo(CURRENT_FOO_VERSION);
            assertThat(artifact.getChecksum()).isEqualTo(fakeChecksumFor(FOO));
            assertThat(artifact.getType()).isEqualTo(war);
        } finally {
            ARTIFACTORY_MOCK.setRequireAuthorization(false);
        }
    }

    @Test void shouldFetchReleasedArtifact() {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");
        Version version = new Version("1.3.3");

        Artifact artifact = repository.resolveArtifact(groupId, artifactId, version, war, null);

        assertThat(artifact.getGroupId()).isEqualTo(groupId);
        assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
        assertThat(artifact.getVersion()).isEqualTo(version);
        assertThat(artifact.getChecksum()).isEqualTo(JOLOKIA_133_CHECKSUM);
    }

    @Test void shouldFetchSnapshotArtifact() {
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

    @Test void shouldFetchStableVersions() {
        assert JOLOKIA_134_CHECKSUM != null; // make sure it's indexed
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");

        List<Version> versions = repository.listVersions(groupId, artifactId, false);

        assertThat(versions).extracting(Version::toString).contains("1.3.2", "1.3.3", "1.3.4");
        assertThat(versions).extracting(Version::toString).doesNotContain("1.3.4-SNAPSHOT");
    }

    @Test void shouldFetchUnstableVersions() {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");

        List<Version> versions = repository.listVersions(groupId, artifactId, true);

        assertThat(versions).extracting(Version::toString).contains("1.3.4-SNAPSHOT");
        assertThat(versions).extracting(Version::toString).doesNotContain("1.3.4");
    }
}
