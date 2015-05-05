package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder.SizeVariation.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
import static java.util.Collections.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.app.html.builder.Tag;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourcesListHtmlWriter extends AbstractListHtmlBodyWriter<DataSourceConfig> {
    public DataSourcesListHtmlWriter() {
        super(DataSourceConfig.class, DATA_SOURCES);
    }

    @Override
    public void body() {
        Tag listGroup = listGroup();
        append(listGroup.header()).append("\n");
        in();
        sort(getTarget());
        int i = 0;
        for (DataSourceConfig dataSource : getTarget()) {
            append("<li class=\"list-group-item\">\n");
            in();
            append(href(dataSource.getName(), DataSources.path(getUriInfo(), dataSource))).append("\n");
            buttons(dataSource, i++);
            out();
            append("</li>\n");
        }
        append("<li class=\"list-group-item\">\n");
        in();
        append(href("+", DataSources.newDataSource(getUriInfo()))).append("\n");
        out();
        append("</li>\n");
        out();
        append("</ul>\n");
    }

    private void buttons(DataSourceConfig dataSource, int i) {
        form().id("delete-" + i) //
                .action(DataSources.path(getUriInfo(), dataSource)) //
                .hiddenInput("action", "delete") //
                .close();
        buttonGroup() //
                .button().size(XS).style(danger).form("delete-" + i).type("submit").icon("remove").close() //
                .close();
    }
}
