package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Compound.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;
import static java.util.Arrays.*;

import java.util.List;

import lombok.*;

import com.github.t1.deployer.app.html.builder.Compound.CompoundBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
public class Page extends Component {
    public static PageBuilder page() {
        return new PageBuilder();
    }

    public static class PageBuilder {
        private final List<Tag> metas = asList( //
                tag("meta").a("charset", "utf-8").build(), //
                tag("meta").a("http-equiv", "X-UA-Compatible").a("content", "IE=edge").build(), //
                tag("meta").a("name", "viewport").a("content", "width=device-width, initial-scale=1").build() //
                );

        private final List<Component> styleSheets = asList( //
                styleSheet("bootstrap/css/bootstrap.css"), //
                styleSheet("webapp/css/style.css") //
                );

        private Component title;
        private final CompoundBuilder body = compound("\n");
        private final CompoundBuilder scripts = compound( //
                nl(), //
                script("jquery/jquery.js"), //
                script("bootstrap/js/bootstrap.js") //
                );

        public PageBuilder title(Component title) {
            this.title = title;
            return this;
        }

        public PageBuilder body(Component body) {
            this.body.component(body);
            return this;
        }

        public Page build() {
            return new Page(tag("html") //
                    .body(head()) //
                    .body(body()) //
                    .build());
        }

        private Component head() {
            TagBuilder head = tag("head");
            for (Tag meta : metas)
                head.body(meta);
            if (title != null)
                head.body(nl()).body(tag("title").body(title).build());
            head.body(nl());
            for (Component styleSheet : styleSheets)
                head.body(styleSheet);
            return head.build();
        }

        private Component body() {
            return tag("body").classes("container-fluid").style("padding-top: 70px") //
                    .body(this.body.build()) //
                    .body(this.scripts.build()) //
                    .build();
        }
    }

    Component html;

    @Override
    public void writeTo(BuildContext out) {
        out.println("<!DOCTYPE html>");
        html.writeTo(out);
    }

    @Override
    public boolean isMultiLine() {
        return true;
    }
}
