package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.builder.Compound.*;
import static com.github.t1.deployer.app.html.builder.NavBar.*;
import static com.github.t1.deployer.app.html.builder.Page.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;

import java.security.Principal;
import java.util.*;

import com.github.t1.deployer.app.Navigation;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.NavBar.NavBarBuilder;
import com.github.t1.deployer.app.html.builder.NavBar.NavBarBuilder.NavBarItemBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;
import com.github.t1.deployer.app.html.builder.Tags.AppendingComponent;

import lombok.*;

@Value
public class DeployerPage implements Component {
    public static DeployerPageBuilder deployerPage() {
        return new DeployerPageBuilder();
    }

    public static class DeployerPageBuilder extends ComponentBuilder {
        private Component title;
        private Component backLink;
        private final List<Component> bodyComponents = new ArrayList<>();

        public DeployerPageBuilder title(Component title) {
            this.title = title;
            return this;
        }

        public DeployerPageBuilder backLink(Component backLink) {
            this.backLink = backLink;
            return this;
        }

        public DeployerPageBuilder panelBody(ComponentBuilder body) {
            return panelBody(body.build());
        }

        public DeployerPageBuilder panelBody(Component body) {
            this.bodyComponents.add(div().classes("panel-body").body(body).build());
            return this;
        }

        public DeployerPageBuilder body(Component body) {
            this.bodyComponents.add(body);
            return this;
        }

        @Override
        public DeployerPage build() {
            Page.PageBuilder page = page().body(navigation()).body(nl());

            if (title != null)
                page.title(title);

            TagBuilder bodyTag = div().classes("panel", "panel-default");

            if (title != null)
                bodyTag.body(heading(backLink, title)).body(nl());

            for (Component body : bodyComponents)
                bodyTag.body(body);
            page.body(bodyTag);

            page.body(pageFooter());

            return new DeployerPage(page.build());
        }

        public TagBuilder heading(Component backLink, final Component title) {
            TagBuilder h1 = tag("h1");
            if (backLink == null)
                h1.body(title);
            else {
                TagBuilder back = link(backLink).classes("glyphicon", "glyphicon-menu-left").body(text(""));
                h1.body(back).body(new Component() {
                    @Override
                    public void writeTo(BuildContext out) {
                        out.print("");
                        title.writeTo(out);
                        out.appendln();
                    }
                });
            }

            return div().classes("panel-heading").body(h1);
        }

        private NavBarBuilder navigation() {
            NavBarBuilder navbar = navBar().brand("Deployer");
            raml(navbar);
            swagger(navbar);
            for (final Navigation navigation : Navigation.values())
                nav(navbar, navigation);
            return navbar;
        }

        private void raml(NavBarBuilder navbar) {
            navbar.item() //
                    .style("padding: 10px;") //
                    .href(baseUri("doc/api-console.html")) //
                    .img(baseUri("img/raml.png")) //
                    .build();
        }

        private void swagger(NavBarBuilder navbar) {
            navbar.item() //
                    .style("padding: 10px;") //
                    .href(baseUri("swagger-ui/index.html") //
                            .queryParam("url", "/deployer/swagger.yaml") //
                            .fragment(new AppendingComponent<String>() {
                                @Override
                                protected String contentFrom(BuildContext out) {
                                    return "!/" + out.get(Navigation.class).linkName();
                                }
                            }))
                    .img(baseUri("swagger-ui/images/logo_small.png")) //
                    .build();
        }

        private void nav(NavBarBuilder navbar, final Navigation navigation) {
            NavBarItemBuilder item = navbar.item() //
                    .href(NavigationLink.link(navigation));
            if (navigation.title() != null)
                item.title(text(navigation.title()));
            if (navigation.icon() != null)
                item.classes("glyphicon", "glyphicon-" + navigation.icon());
            item.classes(out -> {
                if (navigation == out.get(Navigation.class))
                    out.append("active");
            });
            item.build();
        }

        private Component pageFooter() {
            return new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    Principal principal = out.get(Principal.class);
                    if (principal != null)
                        compound( //
                                nl(), //
                                footer().classes("text-muted", "pull-right") //
                                        .body(text("Principal: " + principal.getName())).build() //
                        ).build().writeTo(out);
                }
            };
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
