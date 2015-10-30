package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.Navigation.*;
import static com.github.t1.deployer.app.html.DeployerComponents.*;
import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.builder.Button.*;
import static com.github.t1.deployer.app.html.builder.ButtonGroup.*;
import static com.github.t1.deployer.app.html.builder.Compound.*;
import static com.github.t1.deployer.app.html.builder.Form.*;
import static com.github.t1.deployer.app.html.builder.Input.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;

import java.util.function.Function;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.*;
import com.github.t1.deployer.app.html.DeployerPage.DeployerPageBuilder;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Button.ButtonBuilder;
import com.github.t1.deployer.app.html.builder.Form.FormBuilder;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourceHtmlWriter extends TextHtmlMessageBodyWriter<DataSourceConfig> {
    private static final String MAIN_FORM_ID = "main";

    private static final Component DATA_SOURCE_LINK = append(
            out -> DataSources.path(out.get(UriInfo.class), out.get(DataSourceConfig.class)));

    private static final Compound EXISTING_DATA_SOURCE_FORM = compound( //
            deleteForm(DATA_SOURCE_LINK, "delete"), //
            mainForm(DATA_SOURCE_LINK), //
            buttonGroup() //
                    .button(submitButton("Update")) //
                    .button(button().style(danger).forForm("delete").body(text("Delete"))) //
    ) //
            .build();

    private static final Compound NEW_DATA_SOURCE_FORM = compound( //
            p("Enter the name of a new data source to configure"), //
            mainForm(NavigationLink.link(datasources)), //
            buttonGroup().button(submitButton("Add")) //
    ).build();

    private static ButtonBuilder submitButton(String label) {
        return button().style(primary).forForm(MAIN_FORM_ID).body(text(label));
    }

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(Navigation.datasources);
    }

    @Override
    protected Component component() {
        return out -> {
            DeployerPageBuilder page = deployerPage() //
                    .title(append(context -> title(context.get(DataSourceConfig.class)))) //
                    .backLink(append(context -> DataSources.base(context.get(UriInfo.class))));
            DataSourceConfig target = out.get(DataSourceConfig.class);
            Compound body = target.isNew() ? NEW_DATA_SOURCE_FORM : EXISTING_DATA_SOURCE_FORM;
            page.panelBody(body);
            page.build().writeTo(out);
        };
    }

    public static String title(DataSourceConfig target) {
        return target.isNew() ? "Add Data-Source" : target.getName();
    }

    private static FormBuilder mainForm(Component action) {
        FormBuilder form = form(MAIN_FORM_ID).horizontal().action(action);
        form.input(input("name").label("Name").value(value(c -> c.getName())).required().autofocus());
        form.input(input("jndiName").label("JNDI-Name").value(value(c -> c.getJndiName())).required());
        form.input(input("driver").label("Driver").value(value(c -> c.getDriver())).required());
        form.input(input("uri").label("URI") //
                .value(value(c -> (c.getUri() == null) ? null : c.getUri().toString())) //
                .required());
        form.input(input("user").label("User-Name").value(value(c -> c.getUser())));
        form.input(input("password").label("Password").value(value(c -> c.getPassword())).type("password"));
        return form;
    }

    private static Component value(Function<DataSourceConfig, String> extractor) {
        return append(context -> extractor.apply(context.get(DataSourceConfig.class)));
    }
}
