package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerComponents.*;
import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder2.Button.*;
import static com.github.t1.deployer.app.html.builder2.ButtonGroup.*;
import static com.github.t1.deployer.app.html.builder2.Compound.*;
import static com.github.t1.deployer.app.html.builder2.Form.*;
import static com.github.t1.deployer.app.html.builder2.Input.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.StyleVariation.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;
import static com.github.t1.deployer.model.DataSourceConfig.*;

import java.net.URI;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.app.html.builder2.*;
import com.github.t1.deployer.app.html.builder2.Tags.AppendingComponent;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourceHtmlWriter extends TextHtmlMessageBodyWriter<DataSourceConfig> {
    private static final String MAIN_FORM_ID = "main";

    private static final Tag DATA_SOURCES_LINK = link(new AppendingComponent<URI>() {
        @Override
        protected URI contentFrom(BuildContext out) {
            return DataSources.base(out.get(UriInfo.class));
        }
    }).body(text("&lt;")).build();

    private static final AppendingComponent<URI> DATA_SOURCE_LINK = new AppendingComponent<URI>() {
        @Override
        protected URI contentFrom(BuildContext out) {
            return DataSources.path(out.get(UriInfo.class), out.get(DataSourceConfig.class));
        }
    };

    private static final Compound EXISTING_DATA_SOURCE_FORM = compound( //
            deleteForm(DATA_SOURCE_LINK, "delete"), //
            form(MAIN_FORM_ID) //
                    .action(DATA_SOURCE_LINK) //
                    .body(input("name").label("Name").value(new AppendingComponent<String>() {
                        @Override
                        protected String contentFrom(BuildContext out) {
                            return out.get(DataSourceConfig.class).getName();
                        }
                    }).build()) //
                    .body(input("uri").label("URI").value(new AppendingComponent<URI>() {
                        @Override
                        protected URI contentFrom(BuildContext out) {
                            return out.get(DataSourceConfig.class).getUri();
                        }
                    }).build()) //
                    .build() //
            , //
            buttonGroup().justified() //
                    .button(button().style(primary).forForm(MAIN_FORM_ID).body(text("Update")).build()) //
                    .button(remove("delete")) //
                    .build()

    ).build();

    private static final Compound NEW_DATA_SOURCE_FORM = compound( //
            tag("p").body(text("Enter the name of a new data source to configure")).build() //
            , //
            form(MAIN_FORM_ID) //
                    .action(DATA_SOURCES.link()) //
                    .body(input("name").label("Name").build()) //
                    .body(input("uri").label("URI").build()) //
                    .build() //
            , //
            buttonGroup().justified() //
                    .button(button().style(primary).forForm(MAIN_FORM_ID).body(text("Add")).build()) //
                    .build() //
            ).build();

    private static final DeployerPage PAGE = jumbotronPage() //
            .title(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    DataSourceConfig target = out.get(DataSourceConfig.class);
                    text(title(target)).writeInlineTo(out);
                }
            }) //
            .body(DATA_SOURCES_LINK) //
            .body(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    DataSourceConfig target = out.get(DataSourceConfig.class);
                    if (isNew(target))
                        NEW_DATA_SOURCE_FORM.writeTo(out);
                    else
                        EXISTING_DATA_SOURCE_FORM.writeTo(out);
                }
            }) //
            .build();

    private static boolean isNew(DataSourceConfig target) {
        return NEW_DATA_SOURCE.equals(target.getName());
    }

    public static String title(DataSourceConfig target) {
        return isNew(target) ? "Add Data-Source" : target.getName();
    }

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(Navigation.DATA_SOURCES);
    }

    @Override
    protected Component component() {
        return PAGE;
    }
}
