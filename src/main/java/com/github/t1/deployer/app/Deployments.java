package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.Deployment.*;

import java.net.URI;
import java.util.*;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;

@Boundary
@Path("/deployments")
public class Deployments {
    public static final String CONTEXT_ROOT = "context-root";

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
        return baseBuilder(uriInfo).path(NEW_DEPLOYMENT_PATH).build();
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
        List<Deployment> deployments = new ArrayList<>();
        for (Deployment deployment : container.getAllDeployments()) {
            deployments.add(withVersion(deployment));
        }
        return deployments;
    }

    private Deployment withVersion(Deployment deployment) {
        CheckSum checkSum = deployment.getCheckSum();
        Deployment byChecksum = isEmpty(checkSum) ? null : repository.getByChecksum(checkSum);
        return deployment.withVersion((byChecksum == null) ? Version.UNKNOWN : byChecksum.getVersion());
    }

    private boolean isEmpty(CheckSum checkSum) {
        return checkSum == null || checkSum.isEmpty();
    }

    @GET
    @Path(NEW_DEPLOYMENT_PATH)
    public Deployment newDeployment() {
        return NEW_DEPLOYMENT;
    }

    @Path("")
    public DeploymentResource deploymentSubResourceByContextRoot(@MatrixParam(CONTEXT_ROOT) ContextRoot contextRoot) {
        Deployment deployment = null;
        if (contextRoot == null) {
            deployment = tentativeDeploymentFor(contextRoot);
        } else {
            deployment = container.getDeploymentFor(contextRoot);
            if (deployment == null) {
                deployment = tentativeDeploymentFor(contextRoot);
            }
        }
        return deploymentResource(withVersion(deployment));
    }

    private Deployment tentativeDeploymentFor(ContextRoot contextRoot) {
        return new Deployment(contextRoot);
    }

    private DeploymentResource deploymentResource(Deployment deployment) {
        return deploymentResources.get().deployment(deployment);
    }
}
