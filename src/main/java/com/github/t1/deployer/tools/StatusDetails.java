package com.github.t1.deployer.tools;

import static javax.ws.rs.core.Response.Status.*;

import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
public class StatusDetails {
    public static WebApplicationException badRequest(String message) {
        return webException(BAD_REQUEST, message);
    }

    public static WebApplicationException notFound(String message) {
        return webException(NOT_FOUND, message);
    }

    public static WebApplicationException webException(Status status, String message) {
        StatusDetails error = new StatusDetails(status, message);
        log.info("{}", error);
        return new WebApplicationException(error.toResponse());
    }

    UUID id = UUID.randomUUID();
    Status status;
    String type;

    public Response toResponse() {
        return Response.status(status).entity(this).build();
    }

    @Override
    public String toString() {
        return status.getStatusCode() + " " + status.getReasonPhrase() + " [" + getId() + "] " + type;
    }
}
