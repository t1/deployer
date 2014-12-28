package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;

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

    @GET
    @Path("*")
    public Response getAllDeployments() {
        List<Deployment> deployments = container.getAllDeployments();
        return Response.ok(deployments).build();
    }

    @Path("")
    public Deployment getDeploymentsByContextRoot(@MatrixParam(CONTEXT_ROOT) String contextRoot) {
        return container.getDeploymentByContextRoot(contextRoot);
    }
}
