package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.LoggerConfig.*;
import static com.github.t1.deployer.model.LoggerPatch.*;
import static com.github.t1.deployer.tools.StatusDetails.*;
import static com.github.t1.log.LogLevel.*;
import io.swagger.annotations.*;
import io.swagger.jaxrs.PATCH;

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

@Api(tags = "loggers")
@Boundary
@Path("/loggers")
public class Loggers {
    @ApiModel
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
    @ApiOperation("list of all loggers")
    public List<LoggerConfig> getAllLoggers() {
        return container.getLoggers();
    }

    @GET
    @Path(NEW_LOGGER)
    @ApiOperation(hidden = true, value = "return a form for new loggers")
    public LoggerConfig newLogger() {
        return new LoggerConfig(NEW_LOGGER, OFF);
    }

    @GET
    @Path("{category}")
    @ApiOperation("one logger by category")
    public LoggerConfig getLogger(@PathParam("category") String category) {
        return (NEW_LOGGER.equals(category)) ? newLogger() : container.getLogger(category);
    }

    @GET
    @Path("{category}/level")
    @ApiOperation(value = "the level of a logger", notes = "**Note**: Doesn't work in Swagger; seems to be a bug.")
    public LogLevel getLevel(@PathParam("category") String category) {
        return getLogger(category).getLevel();
    }

    @PUT
    @Path("{category}/level")
    @ApiOperation("set the category of a logger")
    public void putLevel( //
            @PathParam("category") String category, //
            @ApiParam(value = "must be a valid log level as string, i.e. in quotes.", required = true) LogLevel level) {
        LoggerConfig patched = getLogger(category).copy().level(level).build();
        container.update(patched);
    }

    @POST
    @Path("/")
    @ApiOperation("create new logger")
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
    @ApiOperation("create or update a logger")
    public Response put( //
            @Context UriInfo uriInfo, //
            @NotNull @PathParam("category") String category, //
            @NotNull LoggerConfig logger //
    ) {
        if (logger.getCategory() == null)
            logger = logger.copy().category(category).build();
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
    @ApiOperation(value = "patch level or delete logger", //
            notes = "form field `action` can be:\n" //
                    + "* `patch` to patch the level of the logger (form param `level`) or \n" //
                    + "* `delete` to delete the logger" //
    )
    public Response post( //
            @Context UriInfo uriInfo, //
            @NotNull @PathParam("category") String category, //
            @ApiParam(required = true) @NotNull @FormParam("action") PostLoggerAction action, //
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
            default:
                throw badRequest("unspecified action '" + action + "'");
        }
    }

    @PATCH
    @Path("{category}")
    @ApiOperation("patch the level of a logger")
    public Response patch( //
            @NotNull @PathParam("category") String category, //
            @Valid LoggerPatch patch //
    ) {
        LoggerConfig logger = getLogger(category);
        logger = patch.on(logger);
        container.update(logger);
        return Response.noContent().build();
    }

    @DELETE
    @Path("{category}")
    @ApiOperation("delete a logger")
    public void delete(@PathParam("category") String category) {
        container.remove(getLogger(category));
    }
}
