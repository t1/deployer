package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Tags.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class ButtonGroup extends DelegateComponent {
    public static ButtonGroupBuilder buttonGroup() {
        return new ButtonGroupBuilder();
    }

    public static class ButtonGroupBuilder {
        private boolean wrapButtons = false;
        private final TagBuilder tag = div().multiline().a("role", "group").classes("btn-group");

        public ButtonGroupBuilder justified() {
            wrapButtons = true;
            tag.classes("btn-group-justified");
            return this;
        }

        public ButtonGroupBuilder button(Button button) {
            tag.body((wrapButtons) ? buttonGroup().button(button).build() : button);
            return this;
        }

        public ButtonGroup build() {
            return new ButtonGroup(tag.build());
        }
    }

    private ButtonGroup(Tag component) {
        super(component);
    }
}
