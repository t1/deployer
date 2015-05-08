package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Compound.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class Input extends Component {
    public static InputBuilder input(String idAndName) {
        return new InputBuilder(idAndName);
    }

    public static class InputBuilder {
        private final TagBuilder label = tag("label");
        private final TagBuilder input = tag("input").classes("form-control");

        public InputBuilder(String idAndName) {
            input.a("name", idAndName).id(idAndName).a("required");
            label.a("for", idAndName);
        }

        public InputBuilder label(String label) {
            return label(text(label));
        }

        public InputBuilder label(Component label) {
            this.label.body(label);
            return this;
        }

        public InputBuilder value(Component value) {
            input.a("value", value);
            return this;
        }

        public Input build() {
            return new Input(compound(label.build(), input.build()).build());
        }
    }

    Component component;

    @Override
    public void writeTo(BuildContext out) {
        component.writeTo(out);
    }
}
