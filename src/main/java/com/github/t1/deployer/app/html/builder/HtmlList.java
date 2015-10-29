package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;

import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

import lombok.Value;

@Value
public class HtmlList implements Component {
    public static HtmlListBuilder ul() {
        return new HtmlListBuilder("ul");
    }

    public static HtmlListBuilder ol() {
        return new HtmlListBuilder("ol");
    }

    public static HtmlListBuilder listGroup() {
        return new HtmlListBuilder("ul").classes("list-group");
    }

    public static class HtmlListBuilder {
        private final TagBuilder tag;

        public HtmlListBuilder(String type) {
            this.tag = tag(type);
        }

        public HtmlListBuilder id(String id) {
            tag.id(id);
            return this;
        }

        public HtmlListBuilder classes(String... classes) {
            for (String klass : classes)
                classes(text(klass));
            return this;
        }

        public HtmlListBuilder classes(Component... classes) {
            tag.classes(classes);
            return this;
        }

        public HtmlListBuilder li(Component component, Component... classes) {
            tag.body(tag("li").classes(classes).body(component).build());
            return this;
        }

        public HtmlListBuilder item(ComponentBuilder component) {
            return this.item(component.build());
        }

        public HtmlListBuilder item(Component component) {
            return li(component, text("list-group-item"));
        }

        public HtmlList build() {
            return new HtmlList(tag.build());
        }
    }

    Tag tag;

    @Override
    public void writeTo(BuildContext out) {
        tag.writeTo(out);
    }

    @Override
    public boolean isMultiLine() {
        return true;
    }
}
