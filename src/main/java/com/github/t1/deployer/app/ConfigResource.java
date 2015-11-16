package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.Config.DeploymentListFileConfig.*;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.model.Config;
import com.github.t1.deployer.model.Config.ConfigBuilder;
import com.github.t1.deployer.model.Config.DeploymentListFileConfig.DeploymentListFileConfigBuilder;

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

    @Inject
    Config config;

    @com.github.t1.config.Config(
            description = "Automatically delete all deployments not found in the deployments list.",
            defaultValue = "false")
    Boolean autoUndeploy;

    @GET
    @ApiOperation("read the current config")
    public Config getConfig() {
        ConfigBuilder result = config.toBuilder();
        DeploymentListFileConfigBuilder deploymentListFileConfig = //
                (config.deploymentListFileConfig() == null) //
                        ? deploymentListFileConfig() //
                        : config.deploymentListFileConfig().toBuilder();
        deploymentListFileConfig.autoUndeploy(autoUndeploy);
        result.deploymentListFileConfig(deploymentListFileConfig.build());
        return result.build();
    }
}
