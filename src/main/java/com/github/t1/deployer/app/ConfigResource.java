package com.github.t1.deployer.app;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.model.*;

import io.swagger.annotations.*;

@Api(tags = "config")
@Boundary
@Path("/config")
public class ConfigResource {
    private static UriBuilder baseBuilder(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(ConfigResource.class);
    }

    public static URI base(UriInfo uriInfo) {
        return baseBuilder(uriInfo).build();
    }

    public static URI path(UriInfo uriInfo, DataSourceConfig dataSource) {
        return baseBuilder(uriInfo) //
                .path(dataSource.getName()) //
                .build();
    }

    @Inject
    Config config;

    @GET
    @ApiOperation("read the current config")
    public Config getConfig() {
        return config;
    }
}
