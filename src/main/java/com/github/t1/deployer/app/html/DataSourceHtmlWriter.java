package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.StyleVariation.*;
import static com.github.t1.deployer.model.DataSourceConfig.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourceHtmlWriter extends AbstractHtmlBodyWriter<DataSourceConfig> {
    public DataSourceHtmlWriter() {
        super(DataSourceConfig.class, DATA_SOURCES);
    }

    private boolean isNew() {
        return NEW_DATA_SOURCE.equals(target.getName());
    }

    @Override
    protected String bodyTitle() {
        return isNew() ? "Add Data-Source" : target.getName();
    }

    @Override
    protected String title() {
        return isNew() ? "Add Data-Source" : "Data-Source: " + target.getName();
    }

    @Override
    protected void body() {
        indent().href("&lt", DataSources.base(uriInfo)).nl();

        if (isNew())
            append("<p>Enter the name of a new data source to configure</p>\n");
        else
            form().action(DataSources.path(uriInfo, target)) //
                    .hiddenInput("action", "delete") //
                    .submitIcon("remove", danger) //
                    .close();

        form().action(isNew() ? DataSources.base(uriInfo) : DataSources.path(uriInfo, target)) //
                .input("Name", "name", isNew() ? null : target.getName()) //
                .input("URI", "uri", target.getUri()) //
                .submit(isNew() ? "Add" : "Update") //
                .close();
    }
}
