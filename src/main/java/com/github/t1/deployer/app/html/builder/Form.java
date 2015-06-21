package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;

import java.net.URI;

import lombok.*;

import com.github.t1.deployer.app.html.builder.Input.InputBuilder;
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

    public static class FormBuilder extends ComponentBuilder {
        private final TagBuilder tag = tag("form").attr("method", "POST");
        private boolean horizontal;

        public FormBuilder id(Component id) {
            tag.id(id);
            return this;
        }

        public FormBuilder horizontal() {
            this.horizontal = true;
            tag.classes("form-horizontal");
            return this;
        }

        public FormBuilder action(URI action) {
            tag.attr("action", action.toString());
            return this;
        }

        public FormBuilder action(Component action) {
            tag.attr("action", action);
            return this;
        }

        public FormBuilder input(InputBuilder input) {
            if (horizontal)
                input.horizontal();
            tag.body(input.build());
            return this;
        }

        public FormBuilder input(Input input) {
            tag.body(input);
            return this;
        }

        public FormBuilder body(ComponentBuilder component) {
            return this.body(component.build());
        }

        public FormBuilder body(Component component) {
            tag.body(component);
            return this;
        }

        @Override
        public Form build() {
            return new Form(tag.build());
        }
    }

    public Form(Tag component) {
        super(component);
    }
}
