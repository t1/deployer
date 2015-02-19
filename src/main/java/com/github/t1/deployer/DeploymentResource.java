package com.github.t1.deployer;

import static com.github.t1.deployer.WebException.*;
import static javax.ws.rs.core.MediaType.*;

import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeploymentResource {
    private final Container container;
    private final Repository repository;
    private final Audit audit;

    private final DeploymentsList deploymentsList;
    private final Deployment deployment;

    private List<Deployment> availableVersions;

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
            @FormParam("contextRoot") ContextRoot contextRoot, //
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

    private void check(ContextRoot contextRoot) {
        if (!contextRoot.equals(getContextRoot()))
            throw badRequest("context roots don't match: " + contextRoot + " is not " + getContextRoot());
    }

    private void deploy(CheckSum checkSum) {
        if (checkSum == null)
            throw badRequest("checksum missing");
        Deployment newDeployment = repository.getByChecksum(checkSum);
        audit.deploy(newDeployment.getContextRoot(), newDeployment.getVersion());
        newDeployment.deploy(container, repository);
        deploymentsList.writeDeploymentsList();
    }

    @DELETE
    public void delete() {
        audit.undeploy(getContextRoot(), deployment.getVersion());
        container.undeploy(getName());
        deploymentsList.writeDeploymentsList();
    }

    @GET
    @Path("name")
    public DeploymentName getName() {
        return deployment.getName();
    }

    @GET
    @Path("context-root")
    public ContextRoot getContextRoot() {
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
    public List<Deployment> getAvailableVersions() {
        if (availableVersions == null)
            availableVersions = repository.availableVersionsFor(deployment.getCheckSum());
        return availableVersions;
    }

    @Override
    public String toString() {
        return "Resource:" + deployment + "[" + availableVersions + "]";
    }
}
