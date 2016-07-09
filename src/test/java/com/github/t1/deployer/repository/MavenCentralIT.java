package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.RestContext;
import org.junit.*;

import java.io.BufferedReader;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.RepositoryProducer.*;
import static com.github.t1.rest.RestContext.*;
import static org.assertj.core.api.Assertions.*;

@Ignore
public class MavenCentralIT {
    private static final Checksum JOLOKIA_133_CHECKSUM
            = Checksum.fromString("f6e5786754116cc8e1e9261b2a117701747b1259");
    private static final GroupId ORG_JOLOKIA = new GroupId("org.jolokia");
    private static final ArtifactId JOLOKIA_WAR = new ArtifactId("jolokia-war");
    private static final Version VERSION_1_3_3 = new Version("1.3.3");

    private final RestContext rest = REST.register(REST_ALIAS, DEFAULT_MAVEN_CENTRAL_URI);
    private final Repository repository = new MavenCentralRepository(rest);


    @Test
    public void shouldGetByChecksum() throws Exception {
        Artifact artifact = repository.getByChecksum(JOLOKIA_133_CHECKSUM);

        assertThat(artifact.getGroupId()).isEqualTo(ORG_JOLOKIA);
        assertThat(artifact.getArtifactId()).isEqualTo(JOLOKIA_WAR);
        assertThat(artifact.getVersion()).isEqualTo(VERSION_1_3_3);
        assertThat(artifact.getType()).isEqualTo(war);
        assertThat(artifact.getChecksum()).isEqualTo(JOLOKIA_133_CHECKSUM);
    }

    @Test
    public void shouldLookupArtifact() throws Exception {
        Artifact artifact = repository.lookupArtifact(ORG_JOLOKIA, JOLOKIA_WAR, VERSION_1_3_3, war);

        assertThat(artifact.getGroupId()).isEqualTo(ORG_JOLOKIA);
        assertThat(artifact.getArtifactId()).isEqualTo(JOLOKIA_WAR);
        assertThat(artifact.getVersion()).isEqualTo(VERSION_1_3_3);
        assertThat(artifact.getType()).isEqualTo(war);
        // TODO implement with download cache: assertThat(artifact.getChecksum()).isEqualTo(JOLOKIA_133_CHECKSUM);
    }

    @Test
    public void shouldDownloadLookedUpArtifact() throws Exception {
        Artifact artifact = repository.lookupArtifact(ORG_JOLOKIA, JOLOKIA_WAR, VERSION_1_3_3, pom);

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
