package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;

import java.net.URI;

import lombok.*;

import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class Form extends DelegateComponent {
    public static FormBuilder form(String id) {
        return form(text(id));
    }

    public static FormBuilder form(Component id) {
        return form().id(id);
    }

    public static FormBuilder form() {
        return new FormBuilder();
    }

    public static class FormBuilder {
        private final TagBuilder tag = tag("form").a("method", "POST");
        private boolean group;

        public FormBuilder id(Component id) {
            tag.id(id);
            return this;
        }

        public FormBuilder action(URI action) {
            tag.a("action", action.toString());
            return this;
        }

        public FormBuilder action(Component action) {
            tag.a("action", action);
            return this;
        }

        public FormBuilder body(Input input) {
            if (!input.isHidden())
                group = true;
            tag.body(input);
            return this;
        }

        public FormBuilder body(Component component) {
            group = true;
            tag.body(component);
            return this;
        }

        public FormBuilder group() {
            group = true;
            return this;
        }

        public FormBuilder nogroup() {
            group = false;
            return this;
        }

        public Form build() {
            if (group)
                tag.classes("form-group");
            return new Form(tag.build());
        }
    }

    public Form(Tag component) {
        super(component);
    }
}
