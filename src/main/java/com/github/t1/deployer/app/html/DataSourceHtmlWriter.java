package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
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
        return NEW_DATA_SOURCE.equals(getTarget().getName());
    }

    @Override
    public String bodyTitle() {
        return isNew() ? "Add Data-Source" : getTarget().getName();
    }

    @Override
    public String title() {
        return isNew() ? "Add Data-Source" : "Data-Source: " + getTarget().getName();
    }

    @Override
    public void body() {
        indent().href("&lt", DataSources.base(getUriInfo())).nl();

        if (isNew())
            append("<p>Enter the name of a new data source to configure</p>\n");
        else
            form().action(DataSources.path(getUriInfo(), getTarget())) //
                    .hiddenInput("action", "delete") //
                    .submitIcon("remove", danger) //
                    .close();

        form().action(isNew() ? DataSources.base(getUriInfo()) : DataSources.path(getUriInfo(), getTarget())) //
                .input("Name", "name", isNew() ? null : getTarget().getName()) //
                .input("URI", "uri", getTarget().getUri()) //
                .submit(isNew() ? "Add" : "Update") //
                .close();
    }
}
