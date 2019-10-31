package com.github.t1.deployer.repository;

import com.github.t1.jaxrsclienttest.JaxRsTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.net.URI;

import static com.github.t1.deployer.repository.RepositoryType.artifactory;
import static com.github.t1.deployer.repository.RepositoryType.mavenCentral;
import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryProducerTest {
    @RegisterExtension static JaxRsTestExtension MOCK = new JaxRsTestExtension(new LocalArtifactoryMock());

    @Path("/")
    public static class LocalArtifactoryMock {
        @GET public String remotecontent() {
            ++artifactoryCalls;
            return "okay";
        }
    }

    private static int artifactoryCalls = 0;

    private final RepositoryProducer producer = new RepositoryProducer();

    @BeforeEach
    public void setUp() { artifactoryCalls = 0; }

    @Test public void shouldUseArtifactory() {
        producer.type = artifactory;
        producer.uri = MOCK.baseUri();

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(ArtifactoryRepository.class);
        assertThat(artifactoryCalls).isEqualTo(0);
    }

    @Test public void shouldUseMavenCentral() {
        producer.type = mavenCentral;
        producer.uri = MOCK.baseUri();

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(MavenCentralRepository.class);
        assertThat(artifactoryCalls).isEqualTo(0);
    }

    @Test public void shouldLookupArtifactory() {
        producer.uri = MOCK.baseUri();

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(ArtifactoryRepository.class);
        assertThat(artifactoryCalls).isEqualTo(1);
    }

    @Test public void shouldFallbackToMavenCentralWhenArtifactoryPathNotFound() {
        producer.uri = MOCK.baseUri().resolve("/unknown-path");

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(MavenCentralRepository.class);
        assertThat(artifactoryCalls).isEqualTo(0);
    }

    @Test public void shouldFallbackToMavenCentralWhenArtifactoryHostNotFound() {
        producer.uri = URI.create("http://unknown");

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(MavenCentralRepository.class);
        assertThat(artifactoryCalls).isEqualTo(0);
    }
}
