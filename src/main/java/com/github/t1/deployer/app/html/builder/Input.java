package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;
import static java.lang.Boolean.*;

import java.util.function.Function;

import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;
import com.github.t1.deployer.model.*;

import lombok.*;

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

    public static <T> InputBuilder input(StringProperty<T> property, Class<T> type) {
        return input(property.id()) //
                .label(property.title()) //
                .value(append(c -> property.get(c.get(type)).orElse(null)));
    }

    public static <T> InputBuilder input(UriProperty<T> property, Class<T> type) {
        return input(property.id()) //
                .type("uri") //
                .label(property.title()) //
                .value(append(c -> property.get(c.get(type)).orElse(null)));
    }

    public static <T> InputBuilder input(BooleanProperty<T> property, Class<T> type) {
        return new InputBuilder() // not form-control!
                .idAndName(property.id()) //
                .type("checkbox") //
                .value(property.id()) //
                .label(property.title()) //
                .description(property.description()) //
                .attr(append(c -> property.get(c.get(type)).orElse(FALSE) ? "checked" : ""));
    }

    public static InputBuilder input(String idAndName) {
        InputBuilder builder = input();
        builder.idAndName(idAndName);
        return builder;
    }

    public static InputBuilder input() {
        InputBuilder builder = new InputBuilder();
        builder.input.classes("form-control");
        return builder;
    }

    public static class InputBuilder implements ComponentBuilder {
        private TagBuilder label;
        private String idAndName;
        private String type = "text";
        private String description;
        private boolean autofocus;
        private boolean required;
        private boolean horizontal;
        private final TagBuilder input = tag("input").attr("type", append(context -> type)).multiline();

        public InputBuilder idAndName(String idAndName) {
            this.idAndName = idAndName;
            if (idAndName != null)
                input.attr("name", idAndName).id(idAndName);
            return this;
        }

        public InputBuilder type(String type) {
            this.type = type;
            return this;
        }

        public InputBuilder action(String action) {
            return action(text(action));
        }

        public InputBuilder action(Component action) {
            input.attr("name", "action").attr("value", action);
            return this;
        }

        public InputBuilder label(String label) {
            return label(text(label));
        }

        public InputBuilder label(Component label) {
            this.label = tag("label").classes("control-label").body(label);
            return this;
        }

        public InputBuilder name(String name) {
            return name(text(name));
        }

        public InputBuilder name(Component name) {
            return attr("name", name);
        }

        public InputBuilder description(String description) {
            this.description = description;
            return this;
        }

        private InputBuilder attr(Component name) {
            input.attr(name);
            return this;
        }

        private InputBuilder attr(String name, Component value) {
            input.attr(name, value);
            return this;
        }

        public InputBuilder placeholder(String placeholder) {
            input.attr("placeholder", placeholder);
            return this;
        }

        public InputBuilder fieldValue(Function<BuildContext, String> extractor) {
            value(append(context -> extractor.apply(context)));
            return this;
        }

        public InputBuilder value(String value) {
            return value(text(value));
        }

        public InputBuilder value(Component value) {
            if (value != null)
                input.attr("value", value);
            return this;
        }

        public InputBuilder autofocus() {
            autofocus = true;
            return this;
        }

        public InputBuilder required() {
            required = true;
            return this;
        }

        public InputBuilder horizontal() {
            this.horizontal = true;
            return this;
        }

        @Override
        public Input build() {
            if (required)
                input.attr("required");
            if (autofocus)
                input.attr("autofocus");
            if (label == null)
                return new Input("hidden".equals(type) //
                        ? input.build() //
                        : div().classes("form-group").body(input.build()).build(), type);
            if (idAndName != null)
                label.attr("for", idAndName);
            TagBuilder body = this.input;
            if (description != null)
                body.body(text(description));
            if ("checkbox".equals(type))
                body = tag("label").body(body.build());
            if (horizontal) {
                label.classes("col-sm-2");
                body = div().classes("col-sm-10").body(body.build());
            }
            if ("checkbox".equals(type))
                body.classes("checkbox");
            return new Input(div().classes("form-group").body(label.build()).body(body.build()).build(), type);
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
