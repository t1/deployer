package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.log.Logged;

@Slf4j
@Path("/deployments")
@Logged(level = INFO)
public class Deployments {
    @Inject
    VersionsGateway versionsGateway;
    @Inject
    DeploymentContainer deploymentsInfo;

    @GET
    public Response getAllDeployments() {
        log.debug("getAllDeployments");
        return Response.ok(deploymentsInfo.getAllDeployments()).build();
    }

    @GET
    @Path("/context-root={context-root}")
    public Response getDeploymentsByContextRoot(@PathParam("context-root") String contextRoot) {
        log.debug("getDeploymentsByContextRoot {}", contextRoot);
        Deployment deployment = deploymentsInfo.getDeploymentByContextRoot(contextRoot);
        log.debug("found {}", deployment);
        return Response.ok(deployment).build();
    }

    @GET
    @Path("/context-root={context-root}/version")
    public Response getDeploymentVersion(@PathParam("context-root") String contextRoot) {
        return Response.ok(deploymentsInfo.getDeploymentByContextRoot(contextRoot).getVersion()).build();
    }

    @GET
    @Path("/context-root={context-root}/available-versions")
    public Response getDeploymentAvailableVersions(@PathParam("context-root") String contextRoot) {
        return Response.ok(deploymentsInfo.getDeploymentByContextRoot(contextRoot).getAvailableVersions()).build();
    }
}
