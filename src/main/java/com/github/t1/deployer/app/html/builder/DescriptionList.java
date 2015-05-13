package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class DescriptionList extends DelegateComponent {
    public static DescriptionListBuilder descriptionList() {
        return new DescriptionListBuilder();
    }

    public static class DescriptionListBuilder {
        @RequiredArgsConstructor
        public static class DescriptionBuilder {
            private final DescriptionListBuilder list;
            private final TagBuilder title = tag("dt");
            private final TagBuilder description = tag("dd");

            public DescriptionBuilder title(String title) {
                this.title.body(text(title));
                return this;
            }

            public DescriptionBuilder style(String value) {
                title.style(value);
                return this;
            }

            public DescriptionBuilder description(Component description) {
                this.description.body(description);
                return this;
            }

            public DescriptionListBuilder build() {
                list.tag.body(title.build());
                list.tag.body(description.build());
                return list;
            }
        }

        private final TagBuilder tag = tag("dl");

        public DescriptionListBuilder horizontal() {
            tag.classes("dl-horizontal");
            return this;
        }

        public DescriptionBuilder title(String title) {
            return new DescriptionBuilder(this).title(title);
        }

        public DescriptionListBuilder nl() {
            tag.body(Static.nl());
            return this;
        }

        public DescriptionList build() {
            return new DescriptionList(tag.build());
        }
    }

    public DescriptionList(Tag component) {
        super(component);
    }
}
