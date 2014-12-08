package com.github.t1.deployer;

import javax.ws.rs.*;

import lombok.*;

@Path("")
@ToString
@RequiredArgsConstructor
public class Deployment {
    private final String contextRoot;
    private final Version version;

    /** required by JAXB, etc. */
    @Deprecated
    public Deployment() {
        this.contextRoot = null;
        this.version = null;
    }

    @GET
    public Deployment getSelf() {
        return this;
    }

    @GET
    @Path("/context-root")
    public String getContextRoot() {
        return contextRoot;
    }

    @GET
    @Path("/version")
    public Version getVersion() {
        return version;
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
        assert contextRoot.startsWith("/");
        return contextRoot.substring(1);
    }
}
