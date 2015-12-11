package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.LoggerConfig.*;
import static com.github.t1.deployer.model.LoggerPatch.*;
import static com.github.t1.log.LogLevel.*;
import static com.github.t1.ramlap.tools.ProblemDetail.*;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.container.LoggerContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.log.LogLevel;

@Boundary
@Path("/loggers")
public class Loggers {
    public enum PostLoggerAction {
        patch,
        delete;
    }

    private static UriBuilder baseBuilder(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(Loggers.class);
    }

    public static URI base(UriInfo uriInfo) {
        return baseBuilder(uriInfo).build();
    }

    public static URI path(UriInfo uriInfo, LoggerConfig logger) {
        return baseBuilder(uriInfo) //
                .path(logger.getCategory()) //
                .build();
    }

    public static URI newLogger(UriInfo uriInfo) {
        return baseBuilder(uriInfo).path(NEW_LOGGER).build();
    }

    @Inject
    LoggerContainer container;
    @Context
    UriInfo uriInfo;

    @GET
    public List<LoggerConfig> getAllLoggers() {
        return container.getLoggers();
    }

    @GET
    @Path(NEW_LOGGER)
    public LoggerConfig newLogger() {
        return new LoggerConfig(NEW_LOGGER, OFF);
    }

    @GET
    @Path("{category}")
    public LoggerConfig getLogger(@PathParam("category") String category) {
        return (NEW_LOGGER.equals(category)) ? newLogger() : container.getLogger(category);
    }

    @GET
    @Path("{category}/level")
    public LogLevel getLevel(@PathParam("category") String category) {
        return getLogger(category).getLevel();
    }

    @PUT
    @Path("{category}/level")
    public void putLevel( //
            @PathParam("category") String category, //
            LogLevel level) {
        LoggerConfig patched = getLogger(category).toBuilder().level(level).build();
        container.update(patched);
    }

    @POST
    @Path("/")
    public Response postNew( //
            @Context UriInfo uriInfo, //
            @NotNull @FormParam("category") String category, //
            @NotNull @FormParam("level") LogLevel level //
    ) {
        LoggerConfig logger = LoggerConfig.builder().category(category).level(level).build();
        put(uriInfo, category, logger);
        return Response.seeOther(Loggers.base(uriInfo)).build(); // redirect to list
    }

    @PUT
    @Path("/{category}")
    public Response put( //
            @Context UriInfo uriInfo, //
            @NotNull @PathParam("category") String category, //
            @NotNull LoggerConfig logger //
    ) {
        if (logger.getCategory() == null)
            logger = logger.toBuilder().category(category).build();
        else if (!category.equals(logger.getCategory()))
            throw badRequest("path category '" + category + "' " //
                    + "and body category '" + logger.getCategory() + "' don't match " //
                    + "(and body category is not null).");
        if (container.hasLogger(logger)) {
            container.update(logger);
            return Response.noContent().build();
        } else {
            container.add(logger);
            return Response.created(Loggers.path(uriInfo, logger)).build();
        }
    }

    @POST
    @Path("{category}")
    public Response post( //
            @Context UriInfo uriInfo, //
            @NotNull @PathParam("category") String category, //
            @NotNull @FormParam("action") PostLoggerAction action, //
            @FormParam("level") LogLevel level //
    ) {
        if (action == null)
            throw badRequest("missing action form param");
        switch (action) {
        case patch:
            return patch(category, loggerPatch().logLevel(level).build());
        case delete:
            delete(category);
            return Response.seeOther(Loggers.base(uriInfo)).build();
        }
        throw new RuntimeException("unreachable code");
    }

    @PATCH
    @Path("{category}")
    public Response patch( //
            @NotNull @PathParam("category") String category, //
            @Valid LoggerPatch patch //
    ) {
        LoggerConfig logger = getLogger(category);
        logger = patch.apply(logger);
        container.update(logger);
        return Response.noContent().build();
    }

    @DELETE
    @Path("{category}")
    public void delete(@PathParam("category") String category) {
        container.remove(getLogger(category));
    }
}
