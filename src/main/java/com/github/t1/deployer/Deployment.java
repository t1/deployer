package com.github.t1.deployer;

import static javax.xml.bind.annotation.XmlAccessType.*;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.*;

import lombok.*;

@ToString
@RequiredArgsConstructor
@XmlRootElement
@XmlAccessorType(FIELD)
public class Deployment {
    @org.codehaus.jackson.annotate.JsonIgnore
    // @com.fasterxml.jackson.annotation.JsonIgnore
    @XmlTransient
    private final DeploymentsContainer container;
    @org.codehaus.jackson.annotate.JsonIgnore
    // @com.fasterxml.jackson.annotation.JsonIgnore
    @XmlTransient
    private final VersionsGateway versionsGateway;

    private final String name;
    private final String contextRoot;
    private final String hash;

    private String version;

    @org.codehaus.jackson.annotate.JsonIgnore
    // @com.fasterxml.jackson.annotation.JsonIgnore
    @XmlTransient
    private List<Version> availableVersions;

    /** required by JAXB, etc. */
    @Deprecated
    public Deployment() {
        this.versionsGateway = null;
        this.container = null;
        this.name = null;
        this.contextRoot = null;
        this.hash = null;
    }

    @GET
    public Deployment self() {
        return this;
    }

    @PUT
    public Response put(InputStream inputStream) {
        container.deploy(contextRoot, inputStream);

        URI uri = URI.create("http://asdf"); // TODO real URI
        return Response.created(uri).build();
    }

    @DELETE
    public void delete() {
        container.undeploy(contextRoot);
    }

    @GET
    @Path("name")
    public String getName() {
        return name;
    }

    @GET
    @Path("context-root")
    public String getContextRoot() {
        return contextRoot;
    }

    @GET
    @Path("version")
    public String getVersion() {
        if (version == null)
            version = versionsGateway.searchByChecksum(hash).toString();
        return version;
    }

    @GET
    @Path("/available-versions")
    public List<Version> getAvailableVersions() {
        if (availableVersions == null)
            availableVersions = versionsGateway.searchVersions(groupId(), artifactId());
        return availableVersions;
    }

    // FIXME real group ids
    public String groupId() {
        return strippedContextRoot() + "-group";
    }

    // FIXME real artifact ids
    public String artifactId() {
        return strippedContextRoot() + "-artifact";
    }

    private String strippedContextRoot() {
        return contextRoot.startsWith("/") ? contextRoot.substring(1) : contextRoot;
    }
}
