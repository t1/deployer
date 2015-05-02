package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
import static java.util.Collections.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourcesListHtmlWriter extends AbstractListHtmlBodyWriter<DataSourceConfig> {
    public DataSourcesListHtmlWriter() {
        super(DataSourceConfig.class, DATA_SOURCES);
    }

    @Override
    public void body() {
        append("<table>\n");
        in();
        sort(getTarget());
        for (DataSourceConfig dataSource : getTarget()) {
            append("<tr><td>");
            href(dataSource.getName(), DataSources.path(getUriInfo(), dataSource));
            rawAppend("</td><td>\n");
            in();
            buttons(dataSource);
            out();
            append("</td></tr>\n");
        }
        append("<tr><td colspan='2'>");
        href("+", DataSources.newDataSource(getUriInfo()));
        rawAppend("</td></tr>\n");
        out();
        append("</table>\n");
    }

    private void buttons(DataSourceConfig dataSource) {
        form().action(DataSources.path(getUriInfo(), dataSource)) //
                .hiddenInput("action", "delete") //
                .submitIcon("remove", danger) //
                .close();
    }
}
