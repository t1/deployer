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
import static com.github.t1.deployer.model.ConfigProperties.*;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.*;
import com.github.t1.deployer.app.html.DeployerPage.DeployerPageBuilder;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Button.ButtonBuilder;
import com.github.t1.deployer.app.html.builder.Form.FormBuilder;
import com.github.t1.deployer.model.*;

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
            ConfigProperties<Config> config = configProperties();
            DeployerPageBuilder page = deployerPage().title(text(config.$title()));
            Config_RepositoryConfigProperties<Config> repository = config.repository();
            Config_ContainerConfigProperties<Config> container = config.container();
            Config_DeploymentListFileConfigProperties<Config> deploymentListFileConfig =
                    config.deploymentListFileConfig();
            FormBuilder form = form(MAIN_FORM_ID).horizontal().action(CONFIG_LINK) //
                    .fieldset(repository.$title(), //
                            input(repository.uri(), Config.class).autofocus(), //
                            input(repository.authentication().username(), Config.class), //
                            input(repository.authentication().password(), Config.class)) //
                    .fieldset(container.$title(), //
                            input(container.uri(), Config.class)) //
                    .fieldset(deploymentListFileConfig.$title(), //
                            input(deploymentListFileConfig.autoUndeploy(), Config.class).required()) //
                            ;
            page.panelBody(compound( //
                    form, //
                    buttonGroup().button(submitButton("Update"))) //
                            .build());
            page.build().writeTo(out);
        };
    }
}
