package com.github.t1.deployer;

import java.io.*;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeploymentResource {
    private final DeploymentsContainer container;
    private final Repository repository;

    private final Deployment deployment;

    private List<Version> availableVersions;

    @GET
    public Deployment self() {
        return deployment;
    }

    @PUT
    public Response put(@Context UriInfo uriInfo, Deployment entity) {
        try (InputStream inputStream = repository.getArtifactInputStream(entity)) {
            container.deploy(deployment.getContextRoot(), inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Response.created(Deployments.path(uriInfo, deployment)).build();
    }

    @DELETE
    public void delete() {
        container.undeploy(deployment.getName());
    }

    @GET
    @Path("name")
    public String getName() {
        return deployment.getName();
    }

    @GET
    @Path("context-root")
    public String getContextRoot() {
        return deployment.getContextRoot();
    }

    @GET
    @Path("version")
    public Version getVersion() {
        return deployment.getVersion();
    }

    @GET
    @Path("/available-versions")
    public List<Version> getAvailableVersions() {
        if (availableVersions == null)
            availableVersions = repository.availableVersionsFor(deployment);
        return availableVersions;
    }
}
