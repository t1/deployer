package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Tag.*;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;

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

    public static TagBuilder link(Component component) {
        return tag("a").a("href", component);
    }

    public static Component styleSheet(String href) {
        return tag("link").a("href", baseUri(href)).a("rel", "stylesheet").build();
    }

    public static Component script(String href) {
        return tag("script").a("src", baseUri(href)).build();
    }

    public static Component baseUri(final String href) {
        return new AppendingComponent<URI>() {
            @Override
            protected URI contentFrom(BuildContext out) {
                return out.get(UriInfo.class).getBaseUriBuilder().path(href).build();
            }
        };
    }
}
