package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

/** Typical Bootstrap components */
public class Components {
    public static TagBuilder buttonGroup() {
        return div().a("role", "group").classes("btn-group");
    }

    public static TagBuilder iconButton(String formId, String icon, String... classes) {
        return tag("button").multiline() //
                .classes("btn", "btn-block").classes(classes) //
                .a("form", formId) //
                .a("type", "submit") //
                .body(tag("span").multiline() //
                        .classes("glyphicon", "glyphicon-" + icon) //
                        .build()) //
        ;
    }
}
