package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.ConfigModel.DeploymentListFileConfig.*;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.model.ConfigModel;
import com.github.t1.deployer.model.ConfigModel.ConfigModelBuilder;
import com.github.t1.deployer.model.ConfigModel.DeploymentListFileConfig.DeploymentListFileConfigBuilder;

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
    ConfigModel config;

    @com.github.t1.config.Config(
            description = "Automatically delete all deployments not found in the deployments list.",
            defaultValue = "false")
    Boolean autoUndeploy;

    @GET
    public ConfigModel getConfig() {
        ConfigModelBuilder result = config.toBuilder();
        DeploymentListFileConfigBuilder deploymentListFileConfig = //
                (config.deploymentListFileConfig() == null) //
                        ? deploymentListFileConfig() //
                        : config.deploymentListFileConfig().toBuilder();
        deploymentListFileConfig.autoUndeploy(autoUndeploy);
        result.deploymentListFileConfig(deploymentListFileConfig.build());
        return result.build();
    }
}
