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
import com.github.t1.deployer.app.html.builder.Button.ButtonBuilder;
import com.github.t1.deployer.app.html.builder.Form.FormBuilder;
import com.github.t1.deployer.app.html.builder.Input.InputBuilder;
import com.github.t1.deployer.app.html.builder.Tags.AppendingComponent;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourceHtmlWriter extends TextHtmlMessageBodyWriter<DataSourceConfig> {
    private static FormBuilder withFields(boolean withValues, FormBuilder form) {
        form.input(fieldInput(withValues, "name", "Name").required());
        form.input(fieldInput(withValues, "jndiName", "JNDI-Name").required());
        form.input(fieldInput(withValues, "driver", "Driver").required());
        form.input(fieldInput(withValues, "uri", "URI").required());
        form.input(fieldInput(withValues, "user", "User-Name"));
        form.input(fieldInput(withValues, "password", "Password").type("password"));
        return form;
    }

    private static final String MAIN_FORM_ID = "main";

    private static final AppendingComponent<URI> DATA_SOURCE_LINK = new AppendingComponent<URI>() {
        @Override
        protected URI contentFrom(BuildContext out) {
            return DataSources.path(out.get(UriInfo.class), out.get(DataSourceConfig.class));
        }
    };

    private static ButtonBuilder submitButton(String label) {
        return button().style(primary).forForm(MAIN_FORM_ID).body(text(label));
    }

    private static final Compound EXISTING_DATA_SOURCE_FORM = compound( //
            deleteForm(DATA_SOURCE_LINK, "delete"), //
            withFields(true, form(MAIN_FORM_ID).horizontal().action(DATA_SOURCE_LINK)), //
            buttonGroup() //
                    .button(submitButton("Update")) //
                    .button(button().style(danger).forForm("delete").body(text("Delete"))) //
            ) //
            .build();

    private static final Compound NEW_DATA_SOURCE_FORM = //
            compound( //
                    p("Enter the name of a new data source to configure"), //
                    withFields(false, form(MAIN_FORM_ID).horizontal().action(DATA_SOURCES.link())), //
                    buttonGroup().button(submitButton("Add")) //
            ).build();

    private static final DeployerPageBuilder page() {
        return deployerPage() //
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
