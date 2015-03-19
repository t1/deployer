package com.github.t1.deployer;

import static com.github.t1.deployer.WebException.*;
import static javax.ws.rs.core.MediaType.*;

import java.net.URI;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.github.t1.log.Logged;

@Slf4j
@Logged
@RequiredArgsConstructor
public class DeploymentResource {
    @Inject
    Container container;
    @Inject
    Repository repository;
    @Inject
    Audit audit;
    @Inject
    DeploymentsList deploymentsList;

    private Deployment deployment;
    private List<Deployment> availableVersions;

    DeploymentResource deployment(Deployment deployment) {
        this.deployment = deployment;
        return this;
    }

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
        log.debug("post uriInfo={}, action={}, contextRoot={}, checkSum={}", uriInfo, action, contextRoot, checkSum);
        switch (action) {
            case "deploy":
                if (contextRoot != null || getContextRoot() != null)
                    throw badRequest("context root to deploy must be null, not " + contextRoot + " and "
                            + getContextRoot());
                deploy(checkSum);
                Deployment newDeployment = container.getDeploymentWith(checkSum);
                if (newDeployment == null)
                    throw new RuntimeException("deployment didn't work: checksum not found");
                return Response.seeOther(Deployments.path(uriInfo, newDeployment)).build();
            case "redeploy":
                if (getContextRoot() != null)
                    check(contextRoot);
                redeploy(checkSum);
                return Response.seeOther(uriFor(uriInfo, contextRoot)).build();
            case "undeploy":
                if (getContextRoot() != null)
                    check(contextRoot);
                delete();
                return Response.seeOther(Deployments.pathAll(uriInfo)).build();
            default:
                throw badRequest("invalid action '" + action + "'");
        }
    }

    private URI uriFor(UriInfo uriInfo, ContextRoot contextRoot) {
        Deployment newDeployment = container.getDeploymentWith(contextRoot);
        return Deployments.path(uriInfo, newDeployment);
    }

    @PUT
    public Response put(@Context UriInfo uriInfo, Deployment entity) {
        check(entity.getContextRoot());
        CheckSum checkSum = entity.getCheckSum();
        if (checkSum == null)
            throw badRequest("checksum missing in " + entity);
        if (container.hasDeploymentWith(getContextRoot())) {
            redeploy(checkSum);
            return Response.noContent().build();
        } else {
            deploy(checkSum);
            return created(uriInfo);
        }
    }

    private Response created(UriInfo uriInfo) {
        return Response.created(Deployments.path(uriInfo, deployment)).build();
    }

    private void check(ContextRoot contextRoot) {
        if (!Objects.equals(contextRoot, getContextRoot()))
            throw badRequest("context roots don't match: " + contextRoot + " is not " + getContextRoot());
    }

    private void redeploy(CheckSum checkSum) {
        Deployment newDeployment = getDeploymentFromRepository(checkSum);
        audit.redeploy(newDeployment.getContextRoot(), newDeployment.getVersion());
        newDeployment.redeploy(container, repository);
        deploymentsList.writeDeploymentsList();
    }

    private void deploy(CheckSum checkSum) {
        Deployment newDeployment = getDeploymentFromRepository(checkSum);
        audit.deploy(newDeployment.getContextRoot(), newDeployment.getVersion());
        newDeployment.deploy(container, repository);
        deploymentsList.writeDeploymentsList();
    }

    private Deployment getDeploymentFromRepository(CheckSum checkSum) {
        if (checkSum == null)
            throw badRequest("checksum missing");
        Deployment newDeployment = repository.getByChecksum(checkSum);
        if (newDeployment == null)
            throw notFound("no deployment with checksum " + checkSum + " found in repository");
        return newDeployment;
    }

    @DELETE
    public void delete() {
        audit.undeploy(getContextRoot(), deployment.getVersion());
        deployment.undeploy(container);
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
