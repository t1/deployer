package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.Deployment.*;
import static com.github.t1.log.LogLevel.*;

import java.net.URI;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.log.Logged;

@Logged(level = INFO)
@Path("/deployments")
public class Deployments {
    public static final String CONTEXT_ROOT = "context-root";

    private static final Version UNKNOWN_VERSION = new Version("unknown");

    private static UriBuilder baseBuilder(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(Deployments.class);
    }

    public static URI base(UriInfo uriInfo) {
        return baseBuilder(uriInfo).build();
    }

    public static URI pathAll(UriInfo uriInfo) {
        return baseBuilder(uriInfo).path("*").build();
    }

    public static URI path(UriInfo uriInfo, ContextRoot contextRoot) {
        return baseBuilder(uriInfo).matrixParam(CONTEXT_ROOT, contextRoot).build();
    }

    public static URI newDeployment(UriInfo uriInfo) {
        return baseBuilder(uriInfo).path(NEW_DEPLOYMENT_NAME).build();
    }

    @Inject
    DeploymentContainer container;
    @Inject
    Repository repository;
    @Inject
    Instance<DeploymentResource> deploymentResources;
    @Context
    UriInfo uriInfo;

    @GET
    @Path("*")
    public List<Deployment> getAllDeployments() {
        return getAllDeploymentsWithVersions();
    }

    @javax.enterprise.inject.Produces
    List<Deployment> getAllDeploymentsWithVersions() {
        List<Deployment> deployments = container.getAllDeployments();
        for (Deployment deployment : deployments) {
            loadVersion(deployment);
        }
        return deployments;
    }

    private void loadVersion(Deployment deployment) {
        CheckSum checkSum = deployment.getCheckSum();
        Deployment byChecksum = (checkSum == null) ? null : repository.getByChecksum(checkSum);
        deployment.setVersion((byChecksum == null) ? UNKNOWN_VERSION : byChecksum.getVersion());
    }

    @GET
    @Path(NEW_DEPLOYMENT_NAME)
    public DeploymentResource newDeployment() {
        return deploymentResource(NULL_DEPLOYMENT);
    }

    @Path("")
    public DeploymentResource deploymentSubResourceByContextRoot(@MatrixParam(CONTEXT_ROOT) ContextRoot contextRoot) {
        Deployment deployment = null;
        if (contextRoot == null) {
            deployment = tentativeDeploymentFor(contextRoot);
        } else {
            deployment = container.getDeploymentWith(contextRoot);
            if (deployment == null) {
                deployment = tentativeDeploymentFor(contextRoot);
            }
        }
        loadVersion(deployment);
        return deploymentResource(deployment);
    }

    private Deployment tentativeDeploymentFor(ContextRoot contextRoot) {
        return new Deployment(null, contextRoot, null);
    }

    private DeploymentResource deploymentResource(Deployment deployment) {
        return deploymentResources.get().deployment(deployment);
    }
}
