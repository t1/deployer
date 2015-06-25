package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.DataSourceConfig.*;

import java.net.URI;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.container.DataSourceContainer;
import com.github.t1.deployer.model.DataSourceConfig;

@Boundary
@Path("/datasources")
public class DataSources {
    private static UriBuilder baseBuilder(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(DataSources.class);
    }

    public static URI base(UriInfo uriInfo) {
        return baseBuilder(uriInfo).build();
    }

    public static URI path(UriInfo uriInfo, DataSourceConfig dataSource) {
        return baseBuilder(uriInfo) //
                .path(dataSource.getName()) //
                .build();
    }

    public static URI newDataSource(UriInfo uriInfo) {
        return baseBuilder(uriInfo).path(NEW_DATA_SOURCE).build();
    }

    @Inject
    DataSourceContainer container;
    @Inject
    Instance<DataSourceResource> dataSourceResources;

    @GET
    public List<DataSourceConfig> getAllDataSources() {
        return container.getDataSources();
    }

    @GET
    @Path(NEW_DATA_SOURCE)
    public DataSourceConfig newDataSource() {
        return DataSourceConfig.builder().name(NEW_DATA_SOURCE).build();
    }

    @Path("{dataSourceName}")
    public DataSourceResource getDataSource(@PathParam("dataSourceName") String dataSourceName) {
        DataSourceResource dataSourceResource = dataSourceResources.get();
        if (!NEW_DATA_SOURCE.equals(dataSourceName))
            dataSourceResource.dataSource(container.getDataSource(dataSourceName));
        return dataSourceResource;
    }

    @POST
    public Response post(@Context UriInfo uriInfo, @NotNull @FormParam("name") String name) {
        DataSourceConfig newDataSource = DataSourceConfig.builder().name(name).build();
        container.add(newDataSource);
        return Response.seeOther(DataSources.path(uriInfo, newDataSource)).build();
    }
}
