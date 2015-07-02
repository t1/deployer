package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.DataSourceConfig.*;
import io.swagger.annotations.*;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.container.DataSourceContainer;
import com.github.t1.deployer.model.DataSourceConfig;

@Api(tags = "datasources")
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

    @GET
    @ApiOperation("list of all data sources")
    public List<DataSourceConfig> getAllDataSources() {
        return container.getDataSources();
    }

    @GET
    @Path("{dataSourceName}")
    @ApiOperation(value = "a data source by name",
            notes = "The data source name `!` creates a new data source; useful to render a html form.")
    public DataSourceConfig getDataSource(@PathParam("dataSourceName") String dataSourceName) {
        if (NEW_DATA_SOURCE.equals(dataSourceName))
            return DataSourceConfig.builder().name(NEW_DATA_SOURCE).build();
        return container.getDataSource(dataSourceName);
    }

    @POST
    @ApiOperation("post a new data source")
    public Response postNew(@Context UriInfo uriInfo, @NotNull @FormParam("name") String name) {
        DataSourceConfig newDataSource = DataSourceConfig.builder().name(name).build();
        container.add(newDataSource);
        return Response.seeOther(DataSources.path(uriInfo, newDataSource)).build();
    }

    @POST
    @Path("{dataSourceName}")
    @ApiOperation("actions on a data source by name")
    public Response postAction( //
            @Context UriInfo uriInfo, //
            @PathParam("dataSourceName") String dataSourceName, //
            @FormParam("action") String action //
    ) {
        if ("delete".equals(action)) {
            delete(dataSourceName);
            return Response.seeOther(DataSources.base(uriInfo)).build();
        }
        // FIXME other actions
        return Response.noContent().build();
    }

    // TODO patch

    @DELETE
    @Path("{dataSourceName}")
    @ApiOperation("delete a data source by name")
    public void delete(@PathParam("dataSourceName") String dataSourceName) {
        container.remove(dataSourceName);
    }
}
