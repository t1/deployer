package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.log.Logged;

@Slf4j
@Logged(level = INFO)
@Path("/deployments")
public class Deployments {
    @Inject
    VersionsGateway versionsGateway;
    @Inject
    DeploymentsInfo deploymentsInfo;

    @Path("")
    public Response getDeploymentsWithContextRootSubResource(@MatrixParam("context-root") String contextRoot) {
        log.debug("getDeploymentsWithContextRootSubResource {}", contextRoot);
        return getDeploymentsWithContextRoot(contextRoot);
    }

    @GET
    public Response getDeploymentsWithContextRoot(@MatrixParam("context-root") String contextRoot) {
        log.debug("getDeploymentsWithContextRoot {}", contextRoot);
        if (contextRoot == null)
            return Response.ok(deploymentsInfo.getDeployments()).build();
        Deployment deployment = deploymentsInfo.getDeploymentByContextRoot(contextRoot);
        log.debug("found {}", deployment);
        return Response.ok(deployment).build();
    }
}
