package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
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
        sort(target);
        for (DataSourceConfig dataSource : target) {
            append("<tr><td>");
            href(dataSource.getName(), DataSources.path(uriInfo, dataSource));
            out.append("</td><td>\n");
            in();
            buttons(dataSource);
            out();
            append("</td></tr>\n");
        }
        append("<tr><td colspan='2'>");
        href("+", DataSources.newDataSource(uriInfo));
        out.append("</td></tr>\n");
        out();
        append("</table>\n");
    }

    private void buttons(DataSourceConfig dataSource) {
        append("<form method=\"POST\" action=\"" + DataSources.path(uriInfo, dataSource) + "\">\n");
        append("  <input type=\"hidden\" name=\"action\" value=\"delete\">\n");
        append("  <input type=\"submit\" value=\"Delete\">\n");
        append("</form>\n");
    }
}
