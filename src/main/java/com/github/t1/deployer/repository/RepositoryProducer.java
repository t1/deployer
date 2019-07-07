package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Config;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.net.UnknownHostException;

import static com.github.t1.deployer.repository.RepositoryType.artifactory;
import static com.github.t1.deployer.repository.RepositoryType.mavenCentral;
import static com.github.t1.deployer.tools.Tools.nvl;

@Slf4j
@ApplicationScoped
class RepositoryProducer {
    static final URI DEFAULT_ARTIFACTORY_URI = URI.create("http://localhost:8081/artifactory");
    private static final URI DEFAULT_MAVEN_CENTRAL_URI = URI.create("https://search.maven.org");

    @Inject @Config("repository.type") RepositoryType type;
    @Inject @Config("repository.uri") URI uri;
    @Inject @Config("repository.snapshots") String repositorySnapshots;
    @Inject @Config("repository.releases") String repositoryReleases;

    private final Client client = ClientBuilder.newClient();

    @Produces Repository repository() {
        if (type == null)
            type = lookupType();
        switch (type) {
            case mavenCentral:
                return new MavenCentralRepository(client.target(DEFAULT_MAVEN_CENTRAL_URI));
            case artifactory:
                return new ArtifactoryRepository(client.target(artifactoryUri()),
                    nvl(repositorySnapshots, "snapshots-virtual"),
                    nvl(repositoryReleases, "releases-virtual"));
        }
        throw new UnsupportedOperationException("unknown repository type " + type);
    }

    private RepositoryType lookupType() {
        log.debug("lookup repository type");
        URI artifactoryUri = artifactoryUri();
        log.debug("try {}", artifactoryUri);
        if (replies(artifactoryUri)) {
            log.info("{} did reply... choose artifactory", artifactoryUri);
            return artifactory;
        }
        log.info("{} did NOT reply... fall back to maven central", artifactoryUri);
        return mavenCentral;
    }

    private URI artifactoryUri() { return nvl(uri, DEFAULT_ARTIFACTORY_URI); }

    private boolean replies(URI uri) {
        try {
            client.target(uri).request().get();
            return true;
        } catch (RuntimeException e) {
            if ((e.getCause() instanceof UnknownHostException)
                || (e.getCause().getMessage().contains("Connection refused")))
                return false;
            throw e;
        }
    }
}
