package com.github.t1.deployer.app;

import static com.github.t1.log.LogLevel.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.container.DataSourceContainer;
import com.github.t1.deployer.model.DataSourceConfig;
import com.github.t1.log.Logged;

@Logged(level = INFO)
public class DataSourceResource {
    private DataSourceConfig dataSource;

    @Inject
    DataSourceContainer container;

    public DataSourceResource dataSource(DataSourceConfig dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    @GET
    public DataSourceConfig self() {
        return dataSource;
    }

    @GET
    @Path("name")
    public String getLevel() {
        return dataSource.getName();
    }

    @POST
    public Response post(@Context UriInfo uriInfo, @FormParam("action") String action, @FormParam("name") String name) {
        if ("delete".equals(action)) {
            delete();
            return Response.seeOther(DataSources.base(uriInfo)).build();
        }
        container.update(dataSource.setName(name));
        return Response.noContent().build();
    }

    @DELETE
    public void delete() {
        container.remove(dataSource);
    }

    @Override
    @Logged(level = OFF)
    public String toString() {
        return "DataSourceResource:" + dataSource;
    }
}
