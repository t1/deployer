package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import com.github.t1.deployer.app.html.builder.Component.ComponentBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

public class Tags {
    public static abstract class AppendingComponent<T> extends Component {
        @Override
        public void writeTo(BuildContext out) {
            out.append(contentFrom(out));
        }

        protected abstract T contentFrom(BuildContext out);
    }

    public static TagBuilder span() {
        return tag("span");
    }

    public static TagBuilder div() {
        return tag("div");
    }

    public static TagBuilder p(String text) {
        return tag("p").body(text(text));
    }

    public static TagBuilder noscript(ComponentBuilder body) {
        return noscript(body.build());
    }

    public static TagBuilder noscript(Component body) {
        return tag("noscript").body(body);
    }

    public static TagBuilder link(Component component) {
        return tag("a").attr("href", component);
    }

    public static TagBuilder link(URI uri) {
        return tag("a").attr("href", uri.toString());
    }

    public static Component styleSheet(String href) {
        return tag("link").attr("href", baseUri(href)).attr("rel", "stylesheet").build();
    }

    public static Component script(String href) {
        return tag("script").attr("src", baseUri(href)).build();
    }

    public static Component baseUri(final String href) {
        return new AppendingComponent<URI>() {
            @Override
            protected URI contentFrom(BuildContext out) {
                return out.get(UriInfo.class).getBaseUriBuilder().path(href).build();
            }
        };
    }

    public static TagBuilder header(int level) {
        return tag("h" + level);
    }

    public static TagBuilder footer() {
        return tag("footer");
    }

    public static TagBuilder img(String src) {
        return tag("img").attr("src", src);
    }
}
