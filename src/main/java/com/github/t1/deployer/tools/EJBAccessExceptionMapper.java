package com.github.t1.deployer.tools;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

import javax.ejb.EJBAccessException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.*;

@Provider
public class EJBAccessExceptionMapper implements ExceptionMapper<EJBAccessException> {
    @Override
    public Response toResponse(EJBAccessException exception) {
        return Response //
                .status(UNAUTHORIZED) //
                .header("WWW-Authenticate", "Basic realm=\"Deployer\"") //
                .entity(new ErrorResponse(UNAUTHORIZED, exception.getMessage())) //
                .type(APPLICATION_JSON_TYPE) // TODO this prevents JBoss from trying YAML :(
                .build();
    }
}
