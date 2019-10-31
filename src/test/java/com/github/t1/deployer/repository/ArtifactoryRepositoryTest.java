package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.repository.ArtifactoryRepository.MavenMetadata.Versioning.SnapshotVersion;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.t1.deployer.repository.ArtifactoryRepository.MavenMetadata;
import static com.github.t1.deployer.repository.ArtifactoryRepository.artifactFromArtifactoryUri;
import static com.github.t1.deployer.repository.RepositoryProducer.DEFAULT_ARTIFACTORY_URI;
import static org.assertj.core.api.Assertions.assertThat;

class ArtifactoryRepositoryTest {
    private static final Path PATH = Paths.get("/artifactory/api/storage/releases-virtual/"
            + "org/jolokia/jolokia-war/1.3.3/jolokia-war-1.3.3.war");
    private static final URI APP_URI = DEFAULT_ARTIFACTORY_URI.resolve(PATH.toString());

    @Test void shouldDeriveArtifactFromUri() {
        Artifact artifact = artifactFromArtifactoryUri(Checksum.fromString("1234"), APP_URI);

        assertThat(artifact.getGroupId()).hasToString("org.jolokia");
        assertThat(artifact.getArtifactId()).hasToString("jolokia-war");
        assertThat(artifact.getVersion()).hasToString("1.3.3");
        assertThat(artifact.getType()).hasToString("war");
        assertThat(artifact.getChecksum()).hasToString("1234");
    }

    @Test void shouldUnmarshalMavenMetadata() {
        MavenMetadata metadata = JAXB.unmarshal(new StringReader(""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<metadata>\n"
                + "  <groupId>org.jolokia</groupId>\n"
                + "  <artifactId>jolokia-war</artifactId>\n"
                + "  <version>1.3.4-20160805.163054-1</version>\n"
                + "  <versioning>\n"
                + "    <snapshot>\n"
                + "      <timestamp>20160805.163108</timestamp>\n"
                + "      <buildNumber>2</buildNumber>\n"
                + "    </snapshot>\n"
                + "    <lastUpdated>20160805163108</lastUpdated>\n"
                + "    <snapshotVersions>\n"
                + "      <snapshotVersion>\n"
                + "        <extension>pom</extension>\n"
                + "        <value>1.3.4-20160805.163108-2</value>\n"
                + "        <updated>20160805163108</updated>\n"
                + "      </snapshotVersion>\n"
                + "      <snapshotVersion>\n"
                + "        <extension>war</extension>\n"
                + "        <value>1.3.4-20160805.163108-2</value>\n"
                + "        <updated>20160805163108</updated>\n"
                + "      </snapshotVersion>\n"
                + "    </snapshotVersions>\n"
                + "  </versioning>\n"
                + "</metadata>\n"), MavenMetadata.class);

        assertThat(metadata.getVersioning().getSnapshotVersions()).hasSize(2);
        SnapshotVersion v0 = metadata.getVersioning().getSnapshotVersions().get(0);
        assertThat(v0.getExtension()).isEqualTo("pom");
        assertThat(v0.getValue()).isEqualTo("1.3.4-20160805.163108-2");
        assertThat(v0.getUpdated()).isEqualTo("20160805163108");
        SnapshotVersion v1 = metadata.getVersioning().getSnapshotVersions().get(1);
        assertThat(v1.getExtension()).isEqualTo("war");
        assertThat(v1.getValue()).isEqualTo("1.3.4-20160805.163108-2");
        assertThat(v1.getUpdated()).isEqualTo("20160805163108");
    }
}
