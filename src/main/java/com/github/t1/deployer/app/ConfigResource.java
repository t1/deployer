package com.github.t1.deployer.app;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.config.ConfigInfo;

@Boundary
@Path("/config")
public class ConfigResource {
    private static UriBuilder baseBuilder(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(ConfigResource.class);
    }

    public static URI base(UriInfo uriInfo) {
        return baseBuilder(uriInfo).build();
    }

    @Inject
    List<ConfigInfo> configs;

    @GET
    public List<ConfigInfo> getConfig() {
        return configs;
    }
}
