package com.github.t1.deployer.app;

import static javax.ws.rs.core.MediaType.*;

import java.net.URI;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.ramlap.ApiResponse;

import io.swagger.annotations.*;
import lombok.*;

@SwaggerDefinition( //
        info = @Info( //
                title = "Deployer", //
                description = "Deploys web archives to a JBoss web container", //
                version = "", //
                license = @License( //
                        name = "Apache License 2.0", //
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html" //
) //
) , //
        basePath = "http://localhost:8080/deployer", //
        externalDocs = @ExternalDocs( //
                value = "see also on github:", //
                url = "http://github.com/t1/deployer" //
) , //
        tags = { //
                @Tag(name = "root", description = "Entry points into the application"), //
                @Tag(name = "deployments", description = "WARs, EARs, etc."), //
                @Tag(name = "loggers", description = "Loggers, log levels, etc."), //
                @Tag(name = "datasources", description = "Database connections, etc."), //
}, //
        consumes = { APPLICATION_JSON, APPLICATION_XML }, //
        produces = { APPLICATION_JSON, APPLICATION_XML } //
)
@Path("/")
@Api(tags = "root")
@Boundary
public class Index {
    private static final int FOUND = 302; // not in JAX-RS 1.1

    @GET
    @ApiOperation(value = "redirect to start page", //
            nickname = "/", //
            notes = "The html start page should be the list of deployments. "
                    + "Calling the root resouce redirects there by responding with `302 Found`.\n\n" //
                    + "**NOTE** The `Try it out` button will follow the redirect and show the list of deployments.")
    @ApiResponse(statusCode = FOUND, title = "redirect to deployments list")
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
        for (Navigation navigation : Navigation.values())
            list.add(Link.builder() //
                    .uri(navigation.uri(uriInfo)) //
                    .rel(navigation.name()) //
                    .title(navigation.title()) //
                    .build());
        return list;
    }
}
