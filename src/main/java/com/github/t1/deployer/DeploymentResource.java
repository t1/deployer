package com.github.t1.deployer;

import static javax.ws.rs.core.MediaType.*;

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

    @GET
    @Produces(TEXT_HTML)
    public String html() {
        return new DeploymentHtmlWriter(this).toString();
    }

    @PUT
    public Response put(@Context UriInfo uriInfo, Deployment entity) {
        try (InputStream inputStream = repository.getArtifactInputStream(entity.getCheckSum())) {
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
    @Path("checksum")
    public CheckSum getCheckSum() {
        return deployment.getCheckSum();
    }

    @GET
    @Path("/available-versions")
    public List<Version> getAvailableVersions() {
        if (availableVersions == null)
            availableVersions = repository.availableVersionsFor(deployment.getCheckSum());
        return availableVersions;
    }
}
