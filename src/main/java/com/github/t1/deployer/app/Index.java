package com.github.t1.deployer.app;

import io.swagger.annotations.*;

import java.net.URI;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import lombok.*;

@Path("/")
@Api(tags = "root")
@SwaggerDefinition( //
        info = @Info( //
                title = "Deployer", //
                description = "Deploys web archives to a JBoss web container", //
                version = "pre-init", //
                license = @License( //
                        name = "Apache License 2.0", //
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html" //
                ) //
        ), //
        externalDocs = @ExternalDocs( //
                value = "see also on github:", //
                url = "http://github.com/t1/deployer" //
        ), tags = { //
        @Tag(name = "root"), //
                @Tag(name = "deployments"), //
                @Tag(name = "loggers"), //
                @Tag(name = "datasources"), //
        })
@Boundary
public class Index {
    private static final int FOUND = 302; // not in JAX-RS 1.1

    @GET
    @ApiOperation(value = "redirect to start page", //
            nickname = "/", //
            notes = "The html start page should be the list of deployments. "
                    + "Calling the root resouce redirects there by responding with `302 Found`.\n\n" //
                    + "**NOTE** The `Try it out` button will follow the redirect and show the list of deployments.")
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
