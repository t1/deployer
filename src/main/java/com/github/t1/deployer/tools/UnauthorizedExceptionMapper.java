package com.github.t1.deployer.tools;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.*;

@Provider
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {
    @Override
    public Response toResponse(UnauthorizedException exception) {
        return Response //
                .status(UNAUTHORIZED) //
                .header("WWW-Authenticate", "Basic realm=\"ManagementRealm\"") //
                .entity(exception.getMessage()) //
                .type(TEXT_PLAIN) //
                .build();

    }
}
