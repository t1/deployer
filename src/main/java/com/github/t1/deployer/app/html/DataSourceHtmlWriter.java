package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
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
        if (isNew()) {
            append("<p>Enter the name of a new data source to configure</p>\n");
            startForm(DataSources.base(uriInfo));
            append("<input name=\"name\"/>\n");
            append("<input name=\"uri\"/>\n");
            endForm("Add", false);
        } else {
            startForm(DataSources.path(uriInfo, target));
            append("<input name=\"name\" value=\"" + target.getName() + "\"/>\n");
            append("<input name=\"uri\" value=\"" + target.getUri() + "\"/>\n");
            endForm("Update", false);
            delete();
        }
    }

    private void delete() {
        startForm(DataSources.path(uriInfo, target));
        hiddenInput("action", "delete");
        endForm("Delete", false);
    }
}
