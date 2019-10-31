package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.ArtifactType;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.Version;
import com.github.t1.problem.WebApplicationApplicationException;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.ClientBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.github.t1.deployer.model.ArtifactType.jar;
import static com.github.t1.deployer.model.ArtifactType.pom;
import static com.github.t1.deployer.model.ArtifactType.war;
import static com.github.t1.deployer.repository.ArtifactoryMock.UNKNOWN_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JDEPEND_291_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_133_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_133_POM_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_WAR;
import static com.github.t1.deployer.testtools.TestData.ORG_JOLOKIA;
import static com.github.t1.deployer.testtools.TestData.VERSION_1_3_3;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

abstract class MavenCentralTestParent {

    protected abstract URI baseUri();

    protected final Repository repository = new MavenCentralRepository(ClientBuilder.newClient().target(baseUri()));

    @Test void shouldLookupByChecksum() {
        Artifact artifact = repository.lookupByChecksum(JOLOKIA_133_CHECKSUM);

        assertJolokia133(artifact, JOLOKIA_133_CHECKSUM);
    }

    @Test void shouldGetByChecksum() {
        Artifact artifact = repository.searchByChecksum(JOLOKIA_133_CHECKSUM);

        assertJolokia133(artifact, JOLOKIA_133_CHECKSUM);
    }

    @Test void shouldFailToGetByUnknownChecksum() {
        Throwable thrown = catchThrowable(() -> repository.searchByChecksum(UNKNOWN_CHECKSUM));

        assertThat(thrown).isInstanceOf(UnknownChecksumException.class).hasMessageContaining(UNKNOWN_CHECKSUM.hexString());
    }

    @Test void shouldGetByPomChecksum() {
        Artifact artifact = repository.searchByChecksum(JOLOKIA_133_POM_CHECKSUM);

        // maven central returns the info for the 'war', even when we search by the checksum of the pom
        // to find out what classifier the checksum is for, we would have to download the checksums for each classifier
        // the use case is currently only for the audit log when undeploying managed deployables,
        // which doesn't include the classifier anyway
        assertJolokia133(artifact, JOLOKIA_133_POM_CHECKSUM);
    }

    @Test void shouldResolveArtifact() {
        Artifact artifact = repository.resolveArtifact(ORG_JOLOKIA, JOLOKIA_WAR, VERSION_1_3_3, war, null);

        assertJolokia133(artifact, JOLOKIA_133_CHECKSUM);
    }

    @Test void shouldResolveArtifactChecksum() {
        Artifact artifact = repository.resolveArtifact(ORG_JOLOKIA, JOLOKIA_WAR, VERSION_1_3_3, war, null);

        assertThat(artifact.getChecksum()).isEqualTo(JOLOKIA_133_CHECKSUM);
    }

    @Test void shouldFailToResolveUnknownArtifactChecksum() {
        WebApplicationApplicationException thrown = catchThrowableOfType(() ->
                repository.resolveArtifact(GroupId.of("unknown"), ArtifactId.of("unknown-war"), VERSION_1_3_3, war, null).getChecksum(),
            WebApplicationApplicationException.class);

        assertThat(thrown.getResponse().getStatusInfo()).isEqualTo(NOT_FOUND);
        assertThat(thrown).hasMessageContaining("artifact not in repository: unknown:unknown-war:1.3.3:war");
    }

    @Test void shouldFailToResolveFailingArtifactChecksum() {
        WebApplicationApplicationException thrown = catchThrowableOfType(() ->
                repository.resolveArtifact(GroupId.of("unknown"), ArtifactId.of("unknown-war"), VERSION_1_3_3, war, null).getChecksum(),
            WebApplicationApplicationException.class);

        assertThat(thrown.getResponse().getStatusInfo()).isEqualTo(NOT_FOUND);
        assertThat(thrown).hasMessageContaining("artifact not in repository: unknown:unknown-war:1.3.3:war");
    }

    @Test void shouldResolveLatestArtifactChecksum() {
        // the latest JDepend version is 2.9.1 from 2005-11-08, and even if there'd ever be a new version, the group id would probably change
        Artifact artifact = repository.resolveArtifact(GroupId.of("jdepend"), ArtifactId.of("jdepend"), Version.of("LATEST"), jar, null);

        assertThat(artifact.getVersion().getValue()).isEqualTo("2.9.1");
        assertThat(artifact.getChecksum()).isEqualTo(JDEPEND_291_CHECKSUM);
    }

    @Test void shouldResolveUnstableArtifactChecksum() {
        // there are no snapshots on Maven Central
        Artifact artifact = repository.resolveArtifact(GroupId.of("jdepend"), ArtifactId.of("jdepend"), Version.of("UNSTABLE"), jar, null);

        assertThat(artifact.getVersion().getValue()).isEqualTo("2.9.1");
        assertThat(artifact.getChecksum()).isEqualTo(JDEPEND_291_CHECKSUM);
    }

    @Test void shouldDownloadLookedUpPom() throws Exception {
        Artifact artifact = repository.resolveArtifact(ORG_JOLOKIA, JOLOKIA_WAR, VERSION_1_3_3, pom, null);

        assertJolokia133PomDownload(artifact);
    }

    @Test void shouldFailToDownloadUnknownPom() {
        WebApplicationApplicationException thrown = catchThrowableOfType(() ->
                repository.resolveArtifact(GroupId.of("unknown"), ArtifactId.of("unknown-war"), VERSION_1_3_3, pom, null).getInputStream(),
            WebApplicationApplicationException.class);

        assertThat(thrown.getResponse().getStatusInfo()).isEqualTo(NOT_FOUND);
        assertThat(thrown).hasMessageContaining("artifact not in repository: unknown:unknown-war:1.3.3:pom");
    }

    @Test void shouldFetchStableVersions() {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");

        List<Version> versions = repository.listVersions(groupId, artifactId, false);

        assertThat(versions).extracting(Version::toString).contains("1.2.3", "1.3.2", "1.3.3", "1.3.4");
        assertThat(versions).extracting(Version::toString).doesNotContain("1.3.4-SNAPSHOT");
    }

    @Test void shouldFetchUnstableVersions() {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");

        List<Version> versions = repository.listVersions(groupId, artifactId, true);

        // there are no SNAPSHOTs in maven central
        assertThat(versions).extracting(Version::toString).contains("1.2.3", "1.3.2", "1.3.3", "1.3.4");
        assertThat(versions).extracting(Version::toString).doesNotContain("1.3.4-SNAPSHOT");
    }

    private static void assertJolokia133(Artifact artifact, Checksum checksum) {
        assertThat(artifact.getGroupId()).isEqualTo(ORG_JOLOKIA);
        assertThat(artifact.getArtifactId()).isEqualTo(JOLOKIA_WAR);
        assertThat(artifact.getVersion()).isEqualTo(VERSION_1_3_3);
        assertThat(artifact.getType()).isEqualTo(ArtifactType.war);
        assertThat(artifact.getChecksum()).isEqualTo(checksum);
    }

    private static void assertJolokia133PomDownload(Artifact artifact) throws IOException {
        try (BufferedReader reader = new BufferedReader(artifact.getReader())) {
            assertThat(reader.readLine()).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            assertThat(reader.readLine()).isEqualTo("<!--");
            String line;
            do
                line = reader.readLine();
            while (!line.equals("  -->"));
            assertThat(reader.readLine()).isEqualTo("");

            assertThat(reader.readLine()).isEqualTo("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">");
            assertThat(reader.readLine()).isEqualTo("  <modelVersion>4.0.0</modelVersion>");
            assertThat(reader.readLine()).isEqualTo("");
            assertThat(reader.readLine()).isEqualTo("  <groupId>org.jolokia</groupId>");
            assertThat(reader.readLine()).isEqualTo("  <artifactId>jolokia-war</artifactId>");
            assertThat(reader.readLine()).isEqualTo("  <version>1.3.3</version>");
            assertThat(reader.readLine()).isEqualTo("  <name>jolokia-war</name>");
            assertThat(reader.readLine()).isEqualTo("  <packaging>war</packaging>");
            assertThat(reader.readLine()).isEqualTo("  <description>agent as web application</description>");
            assertThat(reader.readLine()).isEqualTo("");
        }
    }
}
