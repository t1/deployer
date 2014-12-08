package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;

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
    DeploymentsInfo deploymentsInfo;

    @Path("")
    public Response getDeploymentsWithContextRootSubResource(@MatrixParam("context-root") String contextRoot) {
        return getDeploymentsWithContextRoot(contextRoot);
    }

    @GET
    public Response getDeploymentsWithContextRoot(@MatrixParam("context-root") String contextRoot) {
        if (contextRoot == null)
            return Response.ok(deploymentsInfo.getDeployments()).build();
        return Response.ok(deploymentsInfo.getDeploymentByContextRoot(contextRoot)).build();
    }
}
