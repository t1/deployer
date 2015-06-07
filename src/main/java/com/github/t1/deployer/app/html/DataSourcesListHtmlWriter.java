package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerComponents.*;
import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.builder.ButtonGroup.*;
import static com.github.t1.deployer.app.html.builder.Compound.*;
import static com.github.t1.deployer.app.html.builder.HtmlList.*;
import static com.github.t1.deployer.app.html.builder.SizeVariation.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;
import static java.util.Collections.*;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Compound.CompoundBuilder;
import com.github.t1.deployer.app.html.builder.HtmlList.HtmlListBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourcesListHtmlWriter extends TextHtmlListMessageBodyWriter<DataSourceConfig> {
    private static final DeployerPage PAGE = deployerPage() //
            .title(text("Data-Sources")) //
            .body(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    HtmlListBuilder ul = listGroup();

                    @SuppressWarnings("unchecked")
                    List<DataSourceConfig> dataSources = out.get(List.class);
                    sort(dataSources);
                    int i = 0;
                    UriInfo uriInfo = out.get(UriInfo.class);
                    for (DataSourceConfig dataSource : dataSources) {
                        ul.item(dataSourceItem(dataSource, uriInfo, i++));
                    }
                    ul.item(addDataSourceItem(uriInfo));

                    ul.build().writeTo(out);
                }

                private CompoundBuilder dataSourceItem(DataSourceConfig dataSource, UriInfo uriInfo, int i) {
                    URI uri = DataSources.path(uriInfo, dataSource);
                    String formId = "delete-" + i;
                    return compound( //
                            span().body(link(uri).body(text(dataSource.getName()))), //
                            span().a("style", "float: right") //
                                    .body(deleteForm(uri, formId)) //
                                    .body(buttonGroup().button(remove(formId, XS))) //
                    );
                }

                private TagBuilder addDataSourceItem(UriInfo uriInfo) {
                    return link(DataSources.newDataSource(uriInfo)).body(ADD_DATA_SOURCE).multiline();
                }
            }) //
            .build();

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(Navigation.DATA_SOURCES);
    }

    @Override
    protected Component component() {
        return PAGE;
    }
}
