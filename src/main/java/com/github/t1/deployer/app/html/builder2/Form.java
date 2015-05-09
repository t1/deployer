package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class Form extends DelegateComponent {
    public static FormBuilder form(String id) {
        return form(text(id));
    }

    public static FormBuilder form(Component id) {
        return new FormBuilder().id(id);
    }

    public static class FormBuilder {
        private final TagBuilder tag = tag("form").a("method", "POST");

        public FormBuilder id(Component id) {
            tag.id(id);
            return this;
        }

        public FormBuilder action(Component action) {
            tag.a("action", action);
            return this;
        }

        public FormBuilder body(Component component) {
            tag.body(component);
            return this;
        }

        public Form build() {
            return new Form(tag.build());
        }
    }

    public Form(Tag component) {
        super(component);
    }
}
