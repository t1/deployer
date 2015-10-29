package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.HtmlList.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;
import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.List;

import com.github.t1.deployer.app.html.builder.HtmlList.HtmlListBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

import lombok.*;

@Value
public class NavBar implements Component {
    public static NavBarBuilder navBar() {
        return new NavBarBuilder();
    }

    public static class NavBarBuilder extends ComponentBuilder {
        public class NavBarItemBuilder {
            private final TagBuilder link = tag("a");
            private final List<Component> classes = new ArrayList<>();

            public NavBarItemBuilder classes(Component... components) {
                this.classes.addAll(asList(components));
                return this;
            }

            public NavBarItemBuilder classes(String... classes) {
                this.classes.addAll(asList(classes).stream().map(s -> text(s)).collect(toList()));
                return this;
            }

            public NavBarItemBuilder href(Component href) {
                this.link.attr("href", href);
                return this;
            }

            public NavBarItemBuilder style(String style) {
                this.link.attr("style", style);
                return this;
            }

            public NavBarItemBuilder title(Component title) {
                this.link.body(title);
                return this;
            }

            public NavBarItemBuilder img(String img) {
                this.link.multiline().body(Tags.img(img));
                return this;
            }

            public NavBarItemBuilder img(Component img) {
                this.link.multiline().body(Tags.img(img));
                return this;
            }

            public void build() {
                navbar.li(link.build(), classArray()).build();
            }

            private Component[] classArray() {
                return classes.toArray(new Component[classes.size()]);
            }
        }

        private final TagBuilder tag = tag("nav").classes("navbar", "navbar-default", "navbar-fixed-top");
        private final TagBuilder container = div().classes("container-fluid");
        private final TagBuilder header = div().classes("navbar-header");
        private final HtmlListBuilder navbar = ul().classes("nav", "navbar-nav", "navbar-right");

        public NavBarBuilder attr(String key, String value) {
            tag.attr(key, value);
            return this;
        }

        public NavBarBuilder brand(String brand) {
            header.body(tag("a").multiline().classes("navbar-brand").body(text(brand)).build());
            return this;
        }

        public NavBarItemBuilder item() {
            return new NavBarItemBuilder();
        }

        @Override
        public NavBar build() {
            tag.body(container //
                    .body(header.build()) //
                    .body(div().id("navbar").classes("navbar-collapse", "collapse") //
                            .body(navbar.build()) //
                            .build()) //
                    .build());
            return new NavBar(tag.build());
        }
    }

    @NonNull
    Tag tag;

    @Override
    public void writeTo(BuildContext out) {
        tag.writeTo(out);
    }

    @Override
    public boolean isMultiLine() {
        return false;
    }
}
