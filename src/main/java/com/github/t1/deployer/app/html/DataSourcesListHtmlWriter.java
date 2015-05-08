package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.builder2.Components.*;
import static com.github.t1.deployer.app.html.builder2.Compound.*;
import static com.github.t1.deployer.app.html.builder2.HtmlList.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;
import static java.util.Collections.*;

import java.util.List;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.app.html.builder2.*;
import com.github.t1.deployer.app.html.builder2.HtmlList.HtmlListBuilder;
import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourcesListHtmlWriter extends TextHtmlListMessageBodyWriter<DataSourceConfig> {
    private static final Static ADD_DATA_SOURCE = text("+");

    private static final DeployerPage PAGE = deployerPage() //
            .title(text("Data-Sources")) //
            .body(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    HtmlListBuilder ul = listGroup();

                    List<DataSourceConfig> dataSources = out.getTarget();
                    sort(dataSources);
                    int i = 0;
                    for (DataSourceConfig dataSource : dataSources) {
                        ul.item(dataSourceItem(dataSource, i++));
                    }
                    ul.item(addDataSourceItem());

                    ul.build().writeTo(out);
                }

                private Compound dataSourceItem(DataSourceConfig dataSource, int i) {
                    UriInfo uriInfo = URI_INFO.get();
                    Static uri = text(DataSources.path(uriInfo, dataSource));
                    String formId = "delete-" + i;
                    return compound() //
                            .component(link(uri).body(text(dataSource.getName())).build()) //
                            .component(deleteForm(uri, formId).build()) //
                            .component(buttonGroup() //
                                    .body(iconButton(formId, "remove", "btn-xs", "btn-danger").build()) //
                                    .build()) //
                            .build();
                }

                private TagBuilder deleteForm(Static uri, String formId) {
                    return tag("form") //
                            .id(formId) //
                            .a("method", "POST") //
                            .a("action", uri) //
                            .body(tag("input").multiline() //
                                    .a("type", "hidden") //
                                    .a("name", "action") //
                                    .a("value", "delete") //
                                    .build());
                }

                private Tag addDataSourceItem() {
                    return link(text(DataSources.newDataSource(URI_INFO.get()))).body(ADD_DATA_SOURCE).multiline()
                            .build();
                }
            }) //
            .build();

    @Override
    protected Component component() {
        return PAGE;
    }
}
