package com.github.t1.deployer.app.html;

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
import com.github.t1.deployer.model.Config;
import com.github.t1.deployer.model.Config.DeploymentListFileConfig;

@Provider
public class ConfigHtmlWriter extends TextHtmlMessageBodyWriter<Config> {
    private static final String MAIN_FORM_ID = "main";

    private static final Component CONFIG_LINK = append(context -> ConfigResource.base(context.get(UriInfo.class)));

    private static ButtonBuilder submitButton(String label) {
        return button().style(primary).forForm(MAIN_FORM_ID).body(text(label));
    }

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(Navigation.config);
    }

    @Override
    protected Component component() {
        return out -> {
            DeployerPageBuilder page = deployerPage().title(text("Configuration"));
            page.panelBody(compound( //
                    form(MAIN_FORM_ID).horizontal().action(CONFIG_LINK) //
                            .input(input("autoUndeploy").label("Auto-Undeploy").value(append(autoUndeploy())).required()
                                    .autofocus()), //
                    buttonGroup().button(submitButton("Update"))) //
                            .build());
            page.build().writeTo(out);
        };
    }

    private static Function<BuildContext, String> autoUndeploy() {
        return context -> {
            Config config = context.get(Config.class);
            DeploymentListFileConfig deploymentListFileConfig = config.deploymentListFileConfig();
            Boolean autoUndeploy = (deploymentListFileConfig == null) ? null : deploymentListFileConfig.autoUndeploy();
            return (autoUndeploy == null) ? "false" : autoUndeploy.toString();
        };
    }
}
