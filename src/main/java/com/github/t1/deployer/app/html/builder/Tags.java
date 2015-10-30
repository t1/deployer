package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.ws.rs.core.*;

import com.github.t1.deployer.app.html.builder.Component.ComponentBuilder;
import com.github.t1.deployer.app.html.builder.Tag.TagBuilder;

public class Tags {
    public static <T> Component append(Function<BuildContext, T> function) {
        return context -> {
            T value = function.apply(context);
            if (value != null)
                context.append(value);
        };
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

    public static class BaseUriBuilder implements Component {
        private String path = "/";
        private final Map<String, String> queryParams = new HashMap<>();
        private Function<BuildContext, String> fragment;

        @Override
        public void writeTo(BuildContext context) {
            UriBuilder uriBuilder = context.get(UriInfo.class).getBaseUriBuilder();
            uriBuilder.path(path);
            for (Entry<String, String> entry : queryParams.entrySet())
                uriBuilder.queryParam(entry.getKey(), entry.getValue());
            if (fragment != null)
                uriBuilder.fragment(fragment.apply(context));
            context.append(uriBuilder.build());
        }

        public BaseUriBuilder path(String path) {
            this.path += path;
            return this;
        }

        public BaseUriBuilder queryParam(String key, String value) {
            queryParams.put(key, value);
            return this;
        }

        public BaseUriBuilder fragment(Function<BuildContext, String> fragment) {
            this.fragment = fragment;
            return this;
        }
    }

    public static BaseUriBuilder baseUri(String path) {
        return new BaseUriBuilder().path(path);
    }

    public static TagBuilder header(int level) {
        return tag("h" + level);
    }

    public static TagBuilder footer() {
        return tag("footer");
    }

    public static TagBuilder img(String src) {
        return img(text(src));
    }

    public static TagBuilder img(Component src) {
        return tag("img").attr("src", src);
    }
}
