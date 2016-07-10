package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.RestContext;
import org.junit.*;

import java.io.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.RepositoryProducer.*;
import static com.github.t1.rest.RestContext.*;
import static org.assertj.core.api.Assertions.*;

@Ignore("requires internet connection")
public class MavenCentralIT {
    private static final Checksum JOLOKIA_133_CHECKSUM
            = Checksum.fromString("f6e5786754116cc8e1e9261b2a117701747b1259");
    private static final Checksum JOLOKIA_133_POM_CHECKSUM
            = Checksum.fromString("a255283f2278ad0fb638d56683a456a3ddd7331e");
    private static final GroupId ORG_JOLOKIA = new GroupId("org.jolokia");
    private static final ArtifactId JOLOKIA_WAR = new ArtifactId("jolokia-war");
    private static final Version VERSION_1_3_3 = new Version("1.3.3");

    private final RestContext rest = REST.register(REST_ALIAS, DEFAULT_MAVEN_CENTRAL_URI);
    private final Repository repository = new MavenCentralRepository(rest);


    private static void assertJolokia133(Artifact artifact, ArtifactType type, Checksum checksum) {
        assertThat(artifact.getGroupId()).isEqualTo(ORG_JOLOKIA);
        assertThat(artifact.getArtifactId()).isEqualTo(JOLOKIA_WAR);
        assertThat(artifact.getVersion()).isEqualTo(VERSION_1_3_3);
        assertThat(artifact.getType()).isEqualTo(type);
        assertThat(artifact.getChecksum()).isEqualTo(checksum);
    }

    @Test
    public void shouldGetByChecksum() throws Exception {
        Artifact artifact = repository.getByChecksum(JOLOKIA_133_CHECKSUM);

        assertJolokia133(artifact, war, JOLOKIA_133_CHECKSUM);
    }

    @Test
    public void shouldGetByPomChecksum() throws Exception {
        Artifact artifact = repository.getByChecksum(JOLOKIA_133_POM_CHECKSUM);

        // maven central returns the info for the 'war', even when we search by the checksum of the pom
        // to find out what classifier the checksum is for, we would have to download the checksums for each classifier
        // the use case is currently only for the audit log when undeploying managed artifacts,
        // which doesn't include the classifier anyway
        assertJolokia133(artifact, war, JOLOKIA_133_POM_CHECKSUM);
    }

    @Test
    public void shouldLookupArtifact() throws Exception {
        Artifact artifact = repository.lookupArtifact(ORG_JOLOKIA, JOLOKIA_WAR, VERSION_1_3_3, war);

        assertJolokia133(artifact, war, JOLOKIA_133_CHECKSUM);
    }

    @Test
    public void shouldLookupArtifactChecksum() throws Exception {
        Artifact artifact = repository.lookupArtifact(ORG_JOLOKIA, JOLOKIA_WAR, VERSION_1_3_3, war);

        assertThat(artifact.getChecksum()).isEqualTo(JOLOKIA_133_CHECKSUM);
    }

    @Test
    public void shouldDownloadLookedUpArtifact() throws Exception {
        Artifact artifact = repository.lookupArtifact(ORG_JOLOKIA, JOLOKIA_WAR, VERSION_1_3_3, pom);

        assertJolokiaPomDownload(artifact);
    }

    private static void assertJolokiaPomDownload(Artifact artifact) throws IOException {
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
        }
    }
}
