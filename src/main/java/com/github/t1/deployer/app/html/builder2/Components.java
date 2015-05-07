package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

/** Typical Bootstrap components */
public class Components {
    public static TagBuilder buttonGroup() {
        return div().a("role", "group").classes("btn-group");
    }

    public static Tag iconButton(String formId, String icon) {
        return tag("button").multiline() //
                .classes("btn", "btn-block", "btn-xs", "btn-danger") //
                .a("form", formId) //
                .a("type", "submit") //
                .body(tag("span").multiline() //
                        .classes("glyphicon", "glyphicon-" + icon) //
                        .build()) //
                .build();
    }
}
