package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;
import static javax.ws.rs.core.MediaType.*;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.log.Logged;

@Logged(level = INFO)
@Path("/deployments")
public class Deployments {
    private static final String CONTEXT_ROOT = "context-root";

    public static URI path(UriInfo uriInfo, Deployment deployment) {
        return uriInfo.getBaseUriBuilder() //
                .path(Deployments.class) //
                .matrixParam(CONTEXT_ROOT, deployment.getContextRoot()) //
                .build();
    }

    @Inject
    DeploymentsContainer container;
    @Inject
    Repository repository;

    @GET
    @Path("*")
    @Produces(TEXT_HTML)
    public String getAllDeploymentsAsHtml() {
        return new DeploymentsListHtml(getAllDeploymentsWithVersions()).toString();
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
        deployment.setVersion(repository.getVersionByChecksum(deployment.getCheckSum()));
    }

    @Path("")
    public DeploymentResource getDeploymentsByContextRoot(@MatrixParam(CONTEXT_ROOT) String contextRoot) {
        Deployment deployment = container.getDeploymentByContextRoot(contextRoot);
        return toResource(deployment);
    }

    private DeploymentResource toResource(Deployment deployment) {
        loadVersion(deployment);
        return new DeploymentResource(container, repository, deployment);
    }
}
