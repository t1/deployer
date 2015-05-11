package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Compound.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

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

    public static InputBuilder input(String idAndName) {
        InputBuilder builder = new InputBuilder();
        builder.input.classes("form-control");
        builder.idAndName(idAndName);
        return builder;
    }

    public static class InputBuilder {
        private TagBuilder label;
        private final TagBuilder input = tag("input").multiline();

        public InputBuilder idAndName(String idAndName) {
            input.a("name", idAndName).id(idAndName).a("required");
            label().a("for", idAndName);
            return this;
        }

        private TagBuilder label() {
            if (label == null)
                label = tag("label");
            return label;
        }

        public InputBuilder type(String type) {
            input.a("type", type);
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
            label().body(label);
            return this;
        }

        public InputBuilder name(String name) {
            return name(text(name));
        }

        public InputBuilder name(Component name) {
            input.a("name", name);
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
            if (label == null)
                return new Input(input.build());
            return new Input(compound(label.build(), input.build()).build());
        }
    }

    public Input(Component component) {
        super(component);
    }
}
