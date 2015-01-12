package com.github.t1.deployer;

import static com.github.t1.deployer.WebException.*;
import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeploymentResource {
    private final Container container;
    private final Repository repository;

    private final Deployment deployment;

    private List<Version> availableVersions;

    @GET
    public Deployment self() {
        return deployment;
    }

    @GET
    @Produces(TEXT_HTML)
    public String html(@Context UriInfo uriInfo) {
        return new DeploymentHtmlWriter(uriInfo, this).toString();
    }

    @POST
    public Response post(@Context UriInfo uriInfo, //
            @FormParam("action") String action, //
            @FormParam("contextRoot") String contextRoot, //
            @FormParam("checkSum") CheckSum checkSum //
    ) {
        check(contextRoot);
        switch (action) {
            case "deploy":
                deploy(checkSum);
                return Response.seeOther(Deployments.path(uriInfo, deployment)).build();
            case "undeploy":
                delete();
                return Response.seeOther(Deployments.pathAll(uriInfo)).build();
            default:
                throw badRequest("invalid action '" + action + "'");
        }
    }

    @PUT
    public Response put(@Context UriInfo uriInfo, Deployment entity) {
        check(entity.getContextRoot());
        CheckSum checkSum = entity.getCheckSum();
        if (checkSum == null)
            throw badRequest("checksum missing in " + entity);
        deploy(checkSum);

        return Response.created(Deployments.path(uriInfo, deployment)).build();
    }

    private void check(String contextRoot) {
        if (!contextRoot.equals(getContextRoot()))
            throw badRequest("context roots don't match: " + contextRoot + " is not " + getContextRoot());
    }

    private void deploy(CheckSum checkSum) {
        if (checkSum == null)
            throw badRequest("checksum missing");
        try (InputStream inputStream = repository.getArtifactInputStream(checkSum)) {
            container.deploy(getName(), inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public String toString() {
        return "Resource:" + deployment + "[" + availableVersions + "]";
    }
}
