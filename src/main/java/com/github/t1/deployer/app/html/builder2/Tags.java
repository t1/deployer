package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Tag.*;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

public class Tags {
    public static TagBuilder div() {
        return tag("div");
    }

    public static TagBuilder link(Component component) {
        return tag("a").a("href", component);
    }

    public static Component styleSheet(String href) {
        return tag("link").a("href", href).a("rel", "stylesheet").build();
    }
}
