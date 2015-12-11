package com.github.t1.deployer.app;

import java.net.URI;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import lombok.*;

@Path("/")
@Boundary
public class Index {
    private static final int FOUND = 302; // not in JAX-RS 1.1

    @GET
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
    public List<Link> getIndexList(@Context UriInfo uriInfo) {
        List<Link> list = new ArrayList<>();
        for (Navigation navigation : Navigation.values())
            list.add(Link.builder() //
                    .uri(navigation.uri(uriInfo)) //
                    .rel(navigation.name()) //
                    .title(navigation.title()) //
                    .build());
        return list;
    }
}
