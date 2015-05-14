package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Compound.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class Input extends DelegateComponent {
    public static Input hiddenAction(String action) {
        return hiddenInput().action(action).build();
    }

    public static InputBuilder hiddenInput() {
        return new InputBuilder().type("hidden");
    }

    public static Input hiddenInput(String name, String value) {
        return new InputBuilder().type("hidden").name(name).value(value).build();
    }

    public static InputBuilder input() {
        InputBuilder builder = new InputBuilder();
        builder.input.classes("form-control");
        return builder;
    }

    public static InputBuilder input(String idAndName) {
        InputBuilder builder = input();
        builder.idAndName(idAndName);
        return builder;
    }

    public static class InputBuilder {
        private TagBuilder label;
        private final TagBuilder input = tag("input").multiline();
        private String idAndName;
        private String type;

        public InputBuilder idAndName(String idAndName) {
            this.idAndName = idAndName;
            return this;
        }

        public InputBuilder type(String type) {
            input.a("type", type);
            this.type = type;
            return this;
        }

        public InputBuilder action(String action) {
            return action(text(action));
        }

        public InputBuilder action(Component action) {
            input.a("name", "action").a("value", action);
            return this;
        }

        public InputBuilder label(String label) {
            return label(text(label));
        }

        public InputBuilder label(Component label) {
            this.label = tag("label").body(label);
            return this;
        }

        public InputBuilder name(String name) {
            return name(text(name));
        }

        public InputBuilder name(Component name) {
            input.a("name", name);
            return this;
        }

        public InputBuilder placeholder(String placeholder) {
            input.a("placeholder", placeholder);
            return this;
        }

        public InputBuilder value(String value) {
            return value(text(value));
        }

        public InputBuilder value(Component value) {
            input.a("value", value);
            return this;
        }

        public Input build() {
            if (idAndName != null)
                input.a("name", idAndName).id(idAndName).a("required");
            if (label == null)
                return new Input(input.build(), type);
            if (idAndName != null)
                label.a("for", idAndName);
            return new Input(compound(label.build(), input.build()).build(), type);
        }
    }

    private final String type;

    public Input(Component component, String type) {
        super(component);
        this.type = type;
    }

    public boolean isHidden() {
        return "hidden".equals(getType());
    }
}
