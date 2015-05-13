package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class Button extends DelegateComponent {
    public static ButtonBuilder button() {
        return new ButtonBuilder();
    }

    public static class ButtonBuilder {
        private final TagBuilder tag = tag("button").multiline().classes("btn", "btn-block");

        public ButtonBuilder justified() {
            tag.classes("btn-group-justified");
            return this;
        }

        public ButtonBuilder icon(String icon, String... classes) {
            body(tag("span").multiline() //
                    .classes("glyphicon", "glyphicon-" + icon).classes(classes) //
                    .build());
            return this;
        }

        public ButtonBuilder body(Component body) {
            tag.body(body);
            return this;
        }

        public ButtonBuilder forForm(String formId) {
            return forForm(text(formId));
        }

        public ButtonBuilder forForm(Component formId) {
            tag.a("form", formId).a("type", "submit");
            return this;
        }

        public ButtonBuilder size(SizeVariation size) {
            if (!size.suffix.isEmpty())
                tag.classes("btn" + size.suffix);
            return this;
        }

        public ButtonBuilder style(StyleVariation style) {
            tag.classes("btn-" + style);
            return this;
        }

        public Button build() {
            return new Button(tag.build());
        }
    }

    private Button(Tag component) {
        super(component);
    }
}
