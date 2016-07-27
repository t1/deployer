package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import org.junit.Test;

import java.net.URI;
import java.nio.file.*;

import static com.github.t1.deployer.repository.ArtifactoryRepository.*;
import static com.github.t1.deployer.repository.RepositoryProducer.*;
import static org.assertj.core.api.Assertions.*;

public class ArtifactoryRepositoryTest {
    private static final Path PATH = Paths.get("/artifactory/api/storage/releases-virtual/"
            + "org/jolokia/jolokia-war/1.3.3/jolokia-war-1.3.3.war");
    private static final URI APP_URI = DEFAULT_ARTIFACTORY_URI.resolve(PATH.toString());

    @Test
    public void shouldDeriveArtifactFromUri() throws Exception {
        Artifact artifact = artifactFromArtifactoryUri(Checksum.fromString("1234"), APP_URI);

        assertThat(artifact.getGroupId()).hasToString("org.jolokia");
        assertThat(artifact.getArtifactId()).hasToString("jolokia-war");
        assertThat(artifact.getVersion()).hasToString("1.3.3");
        assertThat(artifact.getType()).hasToString("war");
        assertThat(artifact.getChecksum()).hasToString("1234");
    }
}
