package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.builder.NavBar.*;
import static com.github.t1.deployer.app.html.builder.Page.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.NavBar.NavBarBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class DeployerPage extends Component {
    public static DeployerPageBuilder panelPage() {
        return new DeployerPageBuilder().panel();
    }

    public static DeployerPageBuilder jumbotronPage() {
        return new DeployerPageBuilder().jumbotron();
    }

    public static class DeployerPageBuilder {
        private final Page.PageBuilder page = page();
        private final TagBuilder body = div();
        private TagBuilder titleTag = null;

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

        public DeployerPageBuilder panel() {
            body.classes("panel", "panel-default");
            titleTag = div().classes("panel-heading");
            return this;
        }

        public DeployerPageBuilder jumbotron() {
            body.classes("jumbotron");
            return this;
        }

        public DeployerPageBuilder title(Component title) {
            // TODO this needs refactoring
            page.title(title);
            Tag titleTag = tag("h1").body(title).build();
            if (this.titleTag != null)
                titleTag = this.titleTag.body(titleTag).build();
            body.body(titleTag).body(nl());
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
