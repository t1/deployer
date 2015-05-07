package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.HtmlList.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;
import static java.util.Arrays.*;

import java.util.*;

import lombok.*;

import com.github.t1.deployer.app.html.builder2.HtmlList.HtmlListBuilder;
import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class NavBar extends Component {
    public static NavBarBuilder navBar() {
        return new NavBarBuilder();
    }

    public static class NavBarBuilder {
        public class NavBarItemBuilder {
            private final TagBuilder tag = tag("a");
            private final List<Component> classes = new ArrayList<>();

            public NavBarItemBuilder href(Component href) {
                this.tag.a("href", href);
                return this;
            }

            public NavBarItemBuilder title(Component title) {
                this.tag.body(title);
                return this;
            }

            public NavBarItemBuilder classes(Component... components) {
                this.classes.addAll(asList(components));
                return this;
            }

            public void build() {
                navbar.li(tag.build(), classArray()).build();
            }

            private Component[] classArray() {
                return classes.toArray(new Component[classes.size()]);
            }
        }

        private final TagBuilder tag = tag("nav").classes("navbar", "navbar-default", "navbar-fixed-top");
        private final TagBuilder container = div().classes("container-fluid");
        private final TagBuilder header = div().classes("navbar-header");
        private final HtmlListBuilder navbar = ul().classes("nav", "navbar-nav", "navbar-right");

        public NavBarBuilder attribute(String key, String value) {
            tag.a(key, value);
            return this;
        }

        public NavBarBuilder brand(String brand) {
            header.body(tag("a").multiline().classes("navbar-brand").a("href", "#").body(text(brand)).build());
            return this;
        }

        public NavBarItemBuilder item() {
            return new NavBarItemBuilder();
        }

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
