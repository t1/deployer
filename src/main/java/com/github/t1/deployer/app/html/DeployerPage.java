package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerPage.PageStyle.*;
import static com.github.t1.deployer.app.html.builder.NavBar.*;
import static com.github.t1.deployer.app.html.builder.Page.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;

import java.util.*;

import lombok.*;

import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.NavBar.NavBarBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class DeployerPage extends Component {
    public enum PageStyle {
        panel {
            @Override
            public TagBuilder pageTag() {
                return div().classes("panel", "panel-default");
            }

            @Override
            public Component heading(Component backLink, Component title) {
                TagBuilder heading = div().classes("panel-heading");
                if (backLink != null)
                    heading.body(link(backLink).classes("glyphicon", "glyphicon-menu-left").body(text("")).build());
                return heading.body(super.heading(backLink, title)).build();
            }

            @Override
            public Component panelBody(Component body) {
                return div().classes("panel-body").body(body).build();
            }
        },
        jumbotron {
            @Override
            public TagBuilder pageTag() {
                return div().classes("jumbotron");
            }
        };

        public abstract TagBuilder pageTag();

        public Component heading(@SuppressWarnings("unused") Component backLink, Component title) {
            return tag("h1").body(title).build();
        }

        public Component panelBody(Component body) {
            return body;
        }
    }

    public static DeployerPageBuilder panelPage() {
        return new DeployerPageBuilder().style(panel);
    }

    public static DeployerPageBuilder jumbotronPage() {
        return new DeployerPageBuilder().style(jumbotron);
    }

    public static class DeployerPageBuilder {
        private PageStyle pageStyle;
        private Component backLink;
        private Component title;
        private final List<Component> bodyComponents = new ArrayList<>();

        public DeployerPageBuilder style(PageStyle style) {
            pageStyle = style;
            return this;
        }

        public DeployerPageBuilder backLink(Component backLink) {
            this.backLink = backLink;
            return this;
        }

        public DeployerPageBuilder title(Component title) {
            this.title = title;
            return this;
        }

        public DeployerPageBuilder panelBody(Component body) {
            this.bodyComponents.add(pageStyle.panelBody(body));
            return this;
        }

        public DeployerPageBuilder body(Component body) {
            this.bodyComponents.add(body);
            return this;
        }

        public DeployerPage build() {
            Page.PageBuilder page = page().body(navigation()).body(nl());

            if (title != null)
                page.title(title);

            TagBuilder bodyTag = pageStyle.pageTag();

            if (title != null) {
                bodyTag.body(pageStyle.heading(backLink, title)).body(nl());
            }

            for (Component body : bodyComponents) {
                bodyTag.body(body);
            }
            page.body(bodyTag.build());

            return new DeployerPage(page.build());
        }

        private NavBar navigation() {
            NavBarBuilder navbar = navBar().brand("Deployer");
            for (final Navigation navigation : Navigation.values()) {
                navbar.item() //
                        .href(navigation.link()) //
                        .title(text(navigation.title())) //
                        .classes(new Component() {
                            @Override
                            public void writeTo(BuildContext out) {
                                if (navigation == out.get(Navigation.class))
                                    out.append("active");
                            }
                        }) //
                        .build();
            }
            return navbar.build();
        }
    }

    @NonNull
    Page page;

    @Override
    public void writeTo(BuildContext out) {
        page.writeTo(out);
    }

    @Override
    public boolean isMultiLine() {
        return true;
    }
}
