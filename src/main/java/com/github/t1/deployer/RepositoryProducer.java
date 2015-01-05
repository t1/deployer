package com.github.t1.deployer;

import java.net.URI;

import javax.enterprise.inject.Produces;

public class RepositoryProducer {
    private static final URI ARTIFACTORY = URI.create("http://localhost:8081");

    @Produces
    public Repository produce() {
        return new ArtifactoryRepository(ARTIFACTORY);
    }
}
