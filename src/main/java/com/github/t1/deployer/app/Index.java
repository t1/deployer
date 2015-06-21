package com.github.t1.deployer.app;

import io.swagger.annotations.*;

import java.net.URI;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import lombok.*;

@Api
@Path("/")
public class Index {
    private static final int FOUND = 302; // not in JAX-RS 1.1

    @GET
    @ApiOperation(value = "redirect to start page", //
            nickname = "/", //
            notes = "The html start page should be the list of deployments. "
                    + "Calling the root resouce redirects there by responding with `302 Found`.\n\n" //
                    + "**NOTE** the actual path is the root path `/`, not `1` as Swagger claims.")
    @ApiResponses(@ApiResponse(code = FOUND, message = "redirect to deployments list"))
    public Response redirectToDeploymentsList(@Context UriInfo uriInfo) {
        return Response.status(FOUND).location(Deployments.pathAll(uriInfo)).build();
    }

    /** The JAX-RS Link class requires 2.0 */
    @Value
    @Builder
    static class Link {
        URI uri;
        String rel;
        String title;
    }

    @GET
    @Path("/index")
    @ApiOperation("list the navigation as links")
    public List<Link> getIndexList(@Context UriInfo uriInfo) {
        List<Link> list = new ArrayList<>();
        for (Navigation navigation : Navigation.values()) {
            list.add(Link.builder() //
                    .uri(navigation.uri(uriInfo)) //
                    .rel(navigation.linkName()) //
                    .title(navigation.title()) //
                    .build());
        }
        return list;
    }
}
