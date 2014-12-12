package com.github.t1.deployer;

import static javax.xml.bind.annotation.XmlAccessType.*;

import java.util.List;

import javax.ws.rs.*;
import javax.xml.bind.annotation.*;

import lombok.*;

@ToString
@RequiredArgsConstructor
@XmlRootElement
@XmlAccessorType(FIELD)
public class Deployment {
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
        this.name = null;
        this.contextRoot = null;
        this.hash = null;
    }

    @GET
    public Deployment self() {
        return this;
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
