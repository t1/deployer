package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerComponents.*;
import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder.Button.*;
import static com.github.t1.deployer.app.html.builder.ButtonGroup.*;
import static com.github.t1.deployer.app.html.builder.Compound.*;
import static com.github.t1.deployer.app.html.builder.Form.*;
import static com.github.t1.deployer.app.html.builder.Input.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;

import java.net.URI;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.app.html.DeployerPage.DeployerPageBuilder;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Input.InputBuilder;
import com.github.t1.deployer.app.html.builder.Tags.AppendingComponent;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourceHtmlWriter extends TextHtmlMessageBodyWriter<DataSourceConfig> {
    private static final Compound fields(boolean withValues) {
        return compound(fieldInput(withValues, "name", "Name").required().build(), //
                fieldInput(withValues, "jndiName", "JNDI-Name").required().build(), //
                fieldInput(withValues, "driver", "Driver").required().build(), //
                fieldInput(withValues, "uri", "URI").required().build(), //
                fieldInput(withValues, "user", "User-Name").build(), //
                fieldInput(withValues, "password", "Password").build()) //
                .build();
    }

    private static final String MAIN_FORM_ID = "main";

    private static final AppendingComponent<URI> DATA_SOURCE_LINK = new AppendingComponent<URI>() {
        @Override
        protected URI contentFrom(BuildContext out) {
            return DataSources.path(out.get(UriInfo.class), out.get(DataSourceConfig.class));
        }
    };

    private static Button submitButton(String label) {
        return button().style(primary).forForm(MAIN_FORM_ID).body(text(label)).build();
    }

    private static final Compound EXISTING_DATA_SOURCE_FORM = compound( //
            deleteForm(DATA_SOURCE_LINK, "delete"), //
            form(MAIN_FORM_ID).action(DATA_SOURCE_LINK).body(fields(true)).build(), //
            buttonGroup().justified() //
                    .button(submitButton("Update")) //
                    .button(remove("delete")) //
                    .build()) //
            .build();

    private static final Compound NEW_DATA_SOURCE_FORM = //
            compound( //
                    p("Enter the name of a new data source to configure"), //
                    form(MAIN_FORM_ID) //
                            .action(DATA_SOURCES.link()) //
                            .body(fields(false)) //
                            .build(), //
                    buttonGroup().justified() //
                            .button(submitButton("Add")) //
                            .build() //
            ).build();

    private static final DeployerPageBuilder page() {
        return panelPage() //
                .title(new AppendingComponent<String>() {
                    @Override
                    protected String contentFrom(BuildContext out) {
                        DataSourceConfig target = out.get(DataSourceConfig.class);
                        return title(target);
                    }
                }) //
                .backLink(new AppendingComponent<URI>() {
                    @Override
                    protected URI contentFrom(BuildContext out) {
                        return DataSources.base(out.get(UriInfo.class));
                    }
                });
    }

    private static InputBuilder fieldInput(boolean withValue, String name, String title) {
        InputBuilder input = input(name).label(title);
        if (withValue)
            input.fieldValue(DataSourceConfig.class, name);
        return input;
    }

    public static String title(DataSourceConfig target) {
        return target.isNew() ? "Add Data-Source" : target.getName();
    }

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(Navigation.DATA_SOURCES);
    }

    @Override
    protected Component component() {
        return new Component() {
            @Override
            public void writeTo(BuildContext out) {
                DeployerPageBuilder page = page();
                DataSourceConfig target = out.get(DataSourceConfig.class);
                Compound body = target.isNew() ? NEW_DATA_SOURCE_FORM : EXISTING_DATA_SOURCE_FORM;
                page.panelBody(body);
                page.build().writeTo(out);
            }
        };
    }
}
