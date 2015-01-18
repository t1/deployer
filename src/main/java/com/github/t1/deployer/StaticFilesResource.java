package com.github.t1.deployer;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

import java.io.InputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.github.t1.log.Logged;

@Path("/")
@Logged
public class StaticFilesResource {
    @GET
    @Path("/css/{file-name}")
    public Response getCss(@PathParam("file-name") String fileName) {
        return respond("css", fileName);
    }

    @GET
    @Path("/fonts/{file-name}")
    public Response getFonts(@PathParam("file-name") String fileName) {
        return respond("fonts", fileName);
    }

    @GET
    @Path("/js/{file-name}")
    public Response getJs(@PathParam("file-name") String fileName) {
        return respond("js", fileName);
    }

    @SuppressWarnings("resource")
    private Response respond(String type, String fileName) {
        InputStream result = getClass().getResourceAsStream("/" + type + "/" + fileName);
        if (result == null)
            return Response //
                    .status(NOT_FOUND) //
                    .entity("no static " + type + " resource found: " + fileName) //
                    .type(TEXT_PLAIN) //
                    .build();
        return Response.ok(result).build();
    }
}
