package com.github.t1.deployer;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

import java.security.Principal;

import javax.ejb.EJBAccessException;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.*;

import lombok.Data;

@Provider
public class EJBAccessExceptionMapper implements ExceptionMapper<EJBAccessException> {
    @Data
    public static class ExceptionBody {
        private String exception;
        private String message;
        private String principal;

        public ExceptionBody(Exception e, String principalName) {
            this.exception = e.getClass().getName();
            this.message = e.getLocalizedMessage();
            this.principal = principalName;
        }
    }

    @Inject
    Principal principal;

    @Override
    public Response toResponse(EJBAccessException exception) {
        return Response.status(UNAUTHORIZED).entity(new ExceptionBody(exception, principal.getName()))
                .type(APPLICATION_JSON_TYPE).build();
    }
}
