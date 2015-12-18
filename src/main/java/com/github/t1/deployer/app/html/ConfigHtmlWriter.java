package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.builder.Button.*;
import static com.github.t1.deployer.app.html.builder.ButtonGroup.*;
import static com.github.t1.deployer.app.html.builder.Compound.*;
import static com.github.t1.deployer.app.html.builder.Form.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;
import static java.util.stream.Collectors.*;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.config.ConfigInfo;
import com.github.t1.deployer.app.*;
import com.github.t1.deployer.app.html.DeployerPage.DeployerPageBuilder;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Button.ButtonBuilder;
import com.github.t1.deployer.app.html.builder.Form.FormBuilder;
import com.github.t1.deployer.app.html.builder.Input.InputBuilder;
import com.github.t1.deployer.model.Password;

@Provider
public class ConfigHtmlWriter extends TextHtmlListMessageBodyWriter<ConfigInfo> {
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
        return context -> {
            DeployerPageBuilder page = deployerPage().title(text("Config"));
            FormBuilder form = form(MAIN_FORM_ID).horizontal().action(CONFIG_LINK);
            AtomicBoolean first = new AtomicBoolean(true);
            for (Entry<Class<?>, List<ConfigInfo>> entry : configsByContainer(context).entrySet()) {
                form.fieldset(camelToTitle(entry.getKey().getSimpleName()) + " Config",
                        inputs(entry.getValue(), first));
            }
            page.panelBody(compound(
                    form,
                    buttonGroup().button(submitButton("Update")))
                            .build());
            page.build().writeTo(context);
        };
    }

    private Map<Class<?>, List<ConfigInfo>> configsByContainer(BuildContext context) {
        @SuppressWarnings("unchecked")
        List<ConfigInfo> configs = context.get(List.class);
        return configs.stream().collect(groupingBy(ConfigInfo::getContainer, LinkedHashMap::new, toList()));
    }

    private static String camelToTitle(String in) {
        StringBuilder out = new StringBuilder();
        for (char c : in.toCharArray()) {
            if (out.length() == 0)
                out.append(Character.toUpperCase(c));
            else if (Character.isUpperCase(c))
                out.append(" ").append(c);
            else
                out.append(c);
        }
        return out.toString();
    }

    private InputBuilder[] inputs(List<ConfigInfo> configInfos, AtomicBoolean first) {
        List<InputBuilder> inputs = new ArrayList<>();
        for (ConfigInfo configInfo : configInfos) {
            InputBuilder input = input(configInfo);
            if (first.getAndSet(false))
                input.autofocus();
            inputs.add(input);
        }
        return inputs.toArray(new InputBuilder[inputs.size()]);
    }

    private InputBuilder input(ConfigInfo configInfo) {
        if (Boolean.class == configInfo.getType() || boolean.class == configInfo.getType())
            return new InputBuilder() // not form-control!
                    .idAndName(configInfo.getName())
                    .type("checkbox")
                    .value("true")
                    .label(label(configInfo))
                    .description(configInfo.getDescription())
                    .required()
                    .attr(append(context -> ((Boolean) configInfo.getValue()) ? "checked" : ""));
        else
            return Input.input(configInfo.getName())
                    .type(type(configInfo))
                    .label(label(configInfo))
                    .description(configInfo.getDescription())
                    .value(configInfo.getValue().toString());
    }

    private String type(ConfigInfo configInfo) {
        if (URI.class.isAssignableFrom(configInfo.getType()))
            return "uri";
        if (Password.class.isAssignableFrom(configInfo.getType()))
            return "password";
        return "text";
    }

    private String label(ConfigInfo configInfo) {
        return configInfo.getMeta().getString("label", camelToTitle(configInfo.getName()));
    }
}
