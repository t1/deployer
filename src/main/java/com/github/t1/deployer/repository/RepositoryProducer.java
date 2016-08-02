package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.HttpHostConnectException;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.net.*;

import static com.github.t1.deployer.repository.RepositoryType.*;
import static com.github.t1.deployer.tools.Tools.*;
import static com.github.t1.rest.RestContext.*;

@Slf4j
public class RepositoryProducer {
    static final URI DEFAULT_ARTIFACTORY_URI = URI.create("http://localhost:8081/artifactory");
    static final URI DEFAULT_MAVEN_CENTRAL_URI = URI.create("https://search.maven.org");
    static final String REST_ALIAS = "repository";

    @Inject @Config("repository.type") RepositoryType type;
    @Inject @Config("repository.uri") URI uri;
    @Inject @Config("repository.username") String username;
    @Inject @Config("repository.password") Password password;
    @Inject @Config("repository.snapshots") String repositorySnapshots;
    @Inject @Config("repository.releases") String repositoryReleases;

    RestContext rest = REST;

    @Produces Repository repository() {
        if (type == null)
            type = lookupType();
        switch (type) {
        case mavenCentral:
            return new MavenCentralRepository(mavenCentralContext());
        case artifactory:
            return new ArtifactoryRepository(artifactoryContext(),
                    nvl(repositorySnapshots, "snapshots-virtual"),
                    nvl(repositoryReleases, "releases-virtual"));
        }
        throw new UnsupportedOperationException("unknown repository type " + type);
    }

    private RepositoryType lookupType() {
        log.debug("lookup repository type");
        RestResource resource = artifactoryContext().resource(REST_ALIAS);
        log.debug("try {}", resource.uri());
        if (replies(resource)) {
            log.info("{} did reply... choose artifactory", resource.uri());
            return artifactory;
        }
        log.info("{} did NOT reply... choose maven central", resource.uri());
        return mavenCentral;
    }

    private boolean replies(RestResource resource) {
        try {
            resource.GET_Response();
            return true;
        } catch (RuntimeException e) {
            if ((e.getCause() instanceof UnknownHostException)
                    || (e.getCause() instanceof HttpHostConnectException
                                && e.getCause().getMessage().contains("Connection refused")))
                return false;
            throw e;
        }
    }

    private RestContext mavenCentralContext() { return rest(DEFAULT_MAVEN_CENTRAL_URI); }

    private RestContext artifactoryContext() { return rest(nvl(uri, DEFAULT_ARTIFACTORY_URI)); }

    private RestContext rest(URI baseUri) {
        if (baseUri != null)
            rest = rest.register(REST_ALIAS, baseUri);
        if (username != null && password != null) {
            log.debug("register {} credentials for {}", username, baseUri);
            Credentials credentials = new Credentials(username, password.getValue());
            rest = rest.register(baseUri, credentials);
        }
        return rest;
    }
}
