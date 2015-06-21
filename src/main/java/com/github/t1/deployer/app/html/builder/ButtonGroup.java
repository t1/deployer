package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Tags.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder.Button.ButtonBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class ButtonGroup extends DelegateComponent {
    public static ButtonGroupBuilder buttonGroup() {
        return new ButtonGroupBuilder();
    }

    public static class ButtonGroupBuilder extends ComponentBuilder {
        private boolean wrapButtons = false;
        private final TagBuilder tag = div().multiline().attr("role", "group").classes("btn-group");

        public ButtonGroupBuilder justified() {
            wrapButtons = true;
            tag.classes("btn-group-justified");
            return this;
        }

        public ButtonGroupBuilder button(ButtonBuilder button) {
            return button(button.build());
        }

        public ButtonGroupBuilder button(Button button) {
            tag.body((wrapButtons) ? buttonGroup().button(button).build() : button);
            return this;
        }

        @Override
        public ButtonGroup build() {
            return new ButtonGroup(tag.build());
        }
    }

    private ButtonGroup(Tag component) {
        super(component);
    }
}
