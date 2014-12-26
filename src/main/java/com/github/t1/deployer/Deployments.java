package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.github.t1.log.Logged;

@Logged(level = INFO)
@Path("/deployments")
public class Deployments {
    @Inject
    VersionsGateway versionsGateway;
    @Inject
    DeploymentsContainer container;

    @GET
    @Path("*")
    public Response getAllDeployments() {
        List<Deployment> deployments = container.getAllDeployments();
        return Response.ok(deployments).build();
    }

    @Path("")
    public Deployment getDeploymentsByContextRoot(@MatrixParam("context-root") String contextRoot) {
        return container.getDeploymentByContextRoot(contextRoot);
    }
}
