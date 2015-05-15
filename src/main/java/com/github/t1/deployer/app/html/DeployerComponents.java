package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.builder.Button.*;
import static com.github.t1.deployer.app.html.builder.Form.*;
import static com.github.t1.deployer.app.html.builder.Input.*;
import static com.github.t1.deployer.app.html.builder.SizeVariation.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;

import java.net.URI;

import com.github.t1.deployer.app.html.builder.*;

public class DeployerComponents {
    public static final Component ADD_DATA_SOURCE = text("+");

    public static Button remove(String formId) {
        return remove(formId, M);
    }

    public static Button remove(String formId, SizeVariation size) {
        return button().icon("remove").size(size).style(danger).forForm(formId).build();
    }

    public static Component deleteForm(Component action, String id) {
        return form(id).action(action).input(hiddenAction("delete")).build();
    }

    public static Component deleteForm(URI action, String id) {
        return form(id).action(action).input(hiddenAction("delete")).build();
    }
}
