package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Tag.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class Grid extends DelegateComponent {
    public static GridBuilder row() {
        return new GridBuilder();
    }

    public static class GridBuilder {
        private final TagBuilder tag = tag("div").classes("row");

        public GridBuilder col(int width, Component col) {
            tag.body(tag("div").classes("col-md-" + width).body(col).build());
            return this;
        }

        public GridBuilder col(Component col) {
            tag.body(tag("div").body(col).build());
            return this;
        }

        public GridBuilder body(Component body) {
            tag.body(body);
            return this;
        }

        public Grid build() {
            return new Grid(tag.build());
        }
    }

    public Grid(Tag component) {
        super(component);
    }
}
