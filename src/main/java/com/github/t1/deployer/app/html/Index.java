package com.github.t1.deployer.app.html;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.app.Deployments;

@Path("/")
public class Index {
    private static final int FOUND = 302;

    @GET
    public Response getAllDeploymentsAsHtml(@Context UriInfo uriInfo) {
        return Response.status(FOUND).location(Deployments.pathAll(uriInfo)).build();
    }
}
