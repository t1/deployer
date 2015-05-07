package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.NavigationHref.*;
import static com.github.t1.deployer.app.html.builder2.Components.*;
import static com.github.t1.deployer.app.html.builder2.Compound.*;
import static com.github.t1.deployer.app.html.builder2.HtmlList.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;
import static java.util.Collections.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.app.html.builder2.*;
import com.github.t1.deployer.app.html.builder2.HtmlList.HtmlListBuilder;
import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourcesListHtmlWriter implements MessageBodyWriter<List<DataSourceConfig>> {
    private static final Static ADD_DATA_SOURCE = text("+");

    public static final DeployerPage PAGE = deployerPage() //
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
                                    .body(iconButton(formId, "remove")) //
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

    @Context
    UriInfo uriInfo;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return List.class.isAssignableFrom(type) //
                && genericType instanceof ParameterizedType //
                && ((ParameterizedType) genericType).getActualTypeArguments().length == 1 //
                && ((ParameterizedType) genericType).getActualTypeArguments()[0] instanceof Class //
                && DataSourceConfig.class.isAssignableFrom((Class<?>) //
                        ((ParameterizedType) genericType).getActualTypeArguments()[0]);
    }

    @Override
    public long getSize(List<DataSourceConfig> t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(List<DataSourceConfig> t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(entityStream);
        try {
            NavigationHref.URI_INFO.set(uriInfo);

            PAGE.write(t).to(writer);

            writer.flush();
        } finally {
            NavigationHref.URI_INFO.remove();
        }
    }
}
