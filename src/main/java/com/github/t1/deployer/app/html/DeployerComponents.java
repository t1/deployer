package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.builder.Button.*;
import static com.github.t1.deployer.app.html.builder.Form.*;
import static com.github.t1.deployer.app.html.builder.Input.*;
import static com.github.t1.deployer.app.html.builder.SizeVariation.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;

import java.net.URI;

import com.github.t1.deployer.app.html.builder.Button.ButtonBuilder;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Form.FormBuilder;

public class DeployerComponents {
    public static final Component ADD_DATA_SOURCE = text("+");

    public static ButtonBuilder remove(String formId) {
        return remove(formId, M);
    }

    public static ButtonBuilder remove(String formId, SizeVariation size) {
        return button().icon("remove").size(size).style(danger).forForm(formId);
    }

    public static FormBuilder deleteForm(Component action, String id) {
        return form(id).action(action).input(hiddenAction("delete"));
    }

    public static FormBuilder deleteForm(URI action, String id) {
        return form(id).action(action).input(hiddenAction("delete"));
    }
}
