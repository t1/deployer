package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;
import static javax.ws.rs.core.MediaType.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.log.Logged;

@Logged(level = INFO)
@Path("/deployments")
public class Deployments {
    private static final Version UNKNOWN_VERSION = new Version("unknown");
    public static final String CONTEXT_ROOT = "context-root";

    public static URI path(UriInfo uriInfo, Deployment deployment) {
        return uriInfo.getBaseUriBuilder() //
                .path(Deployments.class) //
                .matrixParam(CONTEXT_ROOT, deployment.getContextRoot()) //
                .build();
    }

    public static URI pathAll(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder() //
                .path(Deployments.class) //
                .path("*") //
                .build();
    }

    @Inject
    Container container;
    @Inject
    Repository repository;
    @Inject
    Audit audit;
    @Inject
    Principal principal;
    @Inject
    private DeploymentsList deploymentsList;

    @GET
    @Path("*")
    @Produces(TEXT_HTML)
    public String getAllDeploymentsAsHtml(@Context UriInfo uriInfo) {
        return new DeploymentsListHtmlWriter(uriInfo, principal, getAllDeploymentsWithVersions()).toString();
    }

    @GET
    @Path("deployment-form")
    @Produces(TEXT_HTML)
    public String getNewDeploymentsForm(@Context UriInfo uriInfo) {
        return new NewDeploymentFormHtmlWriter(uriInfo).toString();
    }

    @GET
    @Path("*")
    public Response getAllDeployments() {
        List<Deployment> deployments = getAllDeploymentsWithVersions();
        return Response.ok(deployments).build();
    }

    private List<Deployment> getAllDeploymentsWithVersions() {
        List<Deployment> deployments = container.getAllDeployments();
        for (Deployment deployment : deployments) {
            loadVersion(deployment);
        }
        return deployments;
    }

    private void loadVersion(Deployment deployment) {
        Deployment byChecksum =
                (deployment.getCheckSum() == null) ? null : repository.getByChecksum(deployment.getCheckSum());
        deployment.setVersion((byChecksum == null) ? UNKNOWN_VERSION : byChecksum.getVersion());
    }

    @Path("")
    public DeploymentResource deploymentSubResourceByContextRoot(@MatrixParam(CONTEXT_ROOT) ContextRoot contextRoot) {
        Deployment deployment = null;
        if (contextRoot == null) {
            deployment = new Deployment(null, contextRoot, null);
        } else {
            deployment = container.getDeploymentWith(contextRoot);
            if (deployment == null) {
                deployment = new Deployment(null, contextRoot, null);
            }
        }
        loadVersion(deployment);
        return deploymentResource(deployment);
    }

    private DeploymentResource deploymentResource(Deployment deployment) {
        return new DeploymentResource(container, repository, audit, deploymentsList, deployment);
    }
}
