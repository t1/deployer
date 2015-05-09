package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Tags.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class ButtonGroup extends DelegateComponent<Tag> {
    public static ButtonGroupBuilder buttonGroup() {
        return new ButtonGroupBuilder();
    }

    public static class ButtonGroupBuilder {
        private final TagBuilder tag = div().multiline().a("role", "group").classes("btn-group");

        public ButtonGroupBuilder justified() {
            tag.classes("btn-group-justified");
            return this;
        }

        public ButtonGroupBuilder button(Button button) {
            tag.body(button);
            return this;
        }

        public ButtonGroupBuilder button(ButtonGroup group) {
            tag.body(group);
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
