package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder.Select.Option.OptionBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class Select extends DelegateComponent {
    public static SelectBuilder select(String name) {
        return new SelectBuilder().name(name);
    }

    public static OptionBuilder option() {
        return new OptionBuilder();
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class Option extends DelegateComponent {
        public static class OptionBuilder {
            private final TagBuilder tag = tag("option");

            public OptionBuilder selected(boolean selected) {
                if (selected)
                    tag.a("selected");
                return this;
            }

            public OptionBuilder body(String body) {
                tag.body(text(body));
                return this;
            }

            public Option build() {
                return new Option(tag.build());
            }
        }

        public Option(Component component) {
            super(component);
        }
    }

    public static class SelectBuilder {
        private final TagBuilder tag = tag("select").multiline();

        public SelectBuilder name(String name) {
            tag.a("name", name);
            return this;
        }

        public SelectBuilder type(String type) {
            tag.a("type", type);
            return this;
        }

        public SelectBuilder autosubmit() {
            tag.a("onchange", "this.form.submit()");
            return this;
        }

        public SelectBuilder option(Option option) {
            tag.body(option);
            return this;
        }

        public Select build() {
            return new Select(tag.build());
        }
    }

    public Select(Component component) {
        super(component);
    }
}
