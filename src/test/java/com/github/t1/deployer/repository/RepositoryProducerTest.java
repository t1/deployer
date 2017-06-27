package com.github.t1.deployer.repository;

import com.github.t1.rest.RestClientMocker;
import org.junit.Test;

import java.net.*;

import static com.github.t1.deployer.repository.RepositoryProducer.*;
import static com.github.t1.rest.RestContext.*;
import static org.assertj.core.api.Assertions.*;

public class RepositoryProducerTest {
    private static final URI DUMMY_URI = URI.create("http://example.nowhere");
    private final RestClientMocker mocker = new RestClientMocker();

    public static Repository createMavenCentralRepository() {
        return new MavenCentralRepository(REST.register("repository", URI.create("https://search.maven.org")));
    }

    public RepositoryProducer createRepositoryProducer() {
        RepositoryProducer producer = new RepositoryProducer();
        producer.rest = mocker.context();
        return producer;
    }

    public void unknownHost(URI uri) {
        mocker.on(uri).GET().produceBody(() -> {
            throw new RuntimeException(new UnknownHostException());
        });
    }

    @Test
    public void shouldFallbackToMavenCentralWhenLocalArtifactoryNotAvailable() throws Throwable {
        unknownHost(DEFAULT_ARTIFACTORY_URI);
        RepositoryProducer producer = createRepositoryProducer();

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(MavenCentralRepository.class);
    }

    @Test
    public void shouldLookupLocalArtifactoryRepository() throws Throwable {
        mocker.on(DEFAULT_ARTIFACTORY_URI).GET().respond("okay");
        RepositoryProducer producer = createRepositoryProducer();

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(ArtifactoryRepository.class);
    }

    @Test
    public void shouldLookupRemoteArtifactoryRepository() throws Throwable {
        mocker.on(DUMMY_URI).GET().respond("okay");
        RepositoryProducer producer = createRepositoryProducer();
        producer.uri = DUMMY_URI;

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(ArtifactoryRepository.class);
    }
}
