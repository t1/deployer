package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.builder2.Button.*;
import static com.github.t1.deployer.app.html.builder2.Form.*;
import static com.github.t1.deployer.app.html.builder2.Input.*;
import static com.github.t1.deployer.app.html.builder2.SizeVariation.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.StyleVariation.*;

import java.net.URI;

import com.github.t1.deployer.app.html.builder2.*;

public class DeployerComponents {
    public static final Component ADD_DATA_SOURCE = text("+");

    public static Button remove(String formId) {
        return remove(formId, M);
    }

    public static Button remove(String formId, SizeVariation size) {
        return button().icon("remove").size(size).style(danger).forForm(formId).build();
    }

    public static Component deleteForm(Component action, String id) {
        return form(id).action(action).body(hiddenAction("delete")).build();
    }

    public static Component deleteForm(URI action, String id) {
        return form(id).action(action).body(hiddenAction("delete")).build();
    }
}
