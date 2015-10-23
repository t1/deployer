package com.github.t1.deployer.tools;

import static javax.ws.rs.core.Response.Status.*;

import javax.ejb.EJBAccessException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.*;

import com.github.t1.ramlap.*;

@Provider
public class EJBAccessExceptionMapper implements ExceptionMapper<EJBAccessException> {
    @ApiResponse(status = UNAUTHORIZED, title = "requires authentication")
    public static class Unauthorized extends ProblemDetail {}

    @Override
    public Response toResponse(EJBAccessException exception) {
        return new Unauthorized().toResponseBuilder() //
                .header("WWW-Authenticate", "Basic realm=\"Deployer\"") //
                .build();
    }
}
