package com.github.t1.deployer.app;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Stateless
@Path("/check")
public class CheckBoundary {
    @Path("/liveness") @GET public String getLiveness() { return "okay"; }

    @Path("/error") @GET public Response getError() {
        return Response
                .serverError()
                .entity("{\"type\":\"test\"}")
                .type(APPLICATION_JSON_TYPE)
                .build();
    }
}
