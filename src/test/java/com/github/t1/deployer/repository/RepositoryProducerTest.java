package com.github.t1.deployer.repository;

import io.dropwizard.testing.junit.DropwizardClientRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryProducerTest {
    @ClassRule
    public static DropwizardClientRule MOCK = new DropwizardClientRule(LocalArtifactoryMock.class);

    @Path("/")
    public static class LocalArtifactoryMock {
        @GET public String remotecontent() {
            ++artifactoryCalls;
            return "okay";
        }
    }

    private static int artifactoryCalls = 0;

    private final RepositoryProducer producer = new RepositoryProducer();

    @Before
    public void setUp() { artifactoryCalls = 0; }

    @Test public void shouldUseArtifactory() {
        producer.uri = MOCK.baseUri();
        producer.type = RepositoryType.artifactory;

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(ArtifactoryRepository.class);
        assertThat(artifactoryCalls).isEqualTo(0);
    }

    @Test public void shouldUseMavenCentral() {
        producer.type = RepositoryType.mavenCentral;
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

    @Test public void shouldFallbackToMavenCentralWhenArtifactoryNotAvailable() {
        producer.uri = URI.create("http://unknown");

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(MavenCentralRepository.class);
        assertThat(artifactoryCalls).isEqualTo(0);
    }
}
