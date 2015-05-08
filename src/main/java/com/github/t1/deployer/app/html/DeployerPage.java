package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.builder2.NavBar.*;
import static com.github.t1.deployer.app.html.builder2.Page.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder2.*;
import com.github.t1.deployer.app.html.builder2.NavBar.NavBarBuilder;
import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class DeployerPage extends Component {
    public static DeployerPageBuilder deployerPage() {
        return new DeployerPageBuilder();
    }

    public static class DeployerPageBuilder {
        private final Page.PageBuilder page = page();
        private final TagBuilder body = div().classes("jumbotron");

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

        public DeployerPageBuilder title(Component title) {
            page.title(title);
            body.body(tag("h1").body(title).build()).body(nl());
            return this;
        }

        public DeployerPageBuilder body(Component body) {
            this.body.body(body);
            return this;
        }

        public DeployerPage build() {
            page.body(navigation()).body(nl()).body(this.body.build());
            return new DeployerPage(page.build());
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
