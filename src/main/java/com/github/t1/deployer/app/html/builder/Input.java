package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;

import java.lang.reflect.Field;

import lombok.*;

import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;
import com.github.t1.deployer.app.html.builder.Tags.AppendingComponent;

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

    public static class InputBuilder extends ComponentBuilder {
        private TagBuilder label;
        private final TagBuilder input = tag("input").attr("type", new AppendingComponent<String>() {
            @Override
            protected String contentFrom(BuildContext out) {
                return type;
            }
        }).multiline();
        private String idAndName;
        private String type = "text";
        private boolean autofocus;
        private boolean required;
        private boolean horizontal;

        public InputBuilder idAndName(String idAndName) {
            this.idAndName = idAndName;
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
            input.attr("name", name);
            return this;
        }

        public InputBuilder placeholder(String placeholder) {
            input.attr("placeholder", placeholder);
            return this;
        }

        public InputBuilder fieldValue(final Class<?> type, final String fieldName) {
            value(new AppendingComponent<String>() {
                @Override
                protected String contentFrom(BuildContext out) {
                    try {
                        final Field field = type.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object target = out.get(type);
                        if (target == null)
                            return "";
                        Object value = field.get(target);
                        return (value == null) ? "" : value.toString();
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return this;
        }

        public InputBuilder value(String value) {
            return value(text(value));
        }

        public InputBuilder value(Component value) {
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
            if (idAndName != null)
                input.attr("name", idAndName).id(idAndName);
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
            TagBuilder input = this.input;
            if (horizontal) {
                label.classes("col-sm-1");
                input = div().classes("col-sm-11").body(input.build());
            }
            return new Input(div().classes("form-group").body(label.build()).body(input.build()).build(), type);
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
