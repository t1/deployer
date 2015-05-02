package com.github.t1.deployer.app.html.builder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseBuilder {
    private final BaseBuilder container;

    private final Map<String, AtomicInteger> ids;
    public final StringBuilder out;
    public final AtomicInteger indent;

    public BaseBuilder() {
        this.container = null;
        this.ids = new HashMap<>();
        this.out = new StringBuilder();
        this.indent = new AtomicInteger();
    }

    public BaseBuilder(BaseBuilder container) {
        this.container = container;
        this.ids = container.ids;
        this.indent = container.indent;
        this.out = container.out;
    }

    public int id(String type) {
        AtomicInteger result = ids.get(type);
        if (result == null) {
            result = new AtomicInteger();
            ids.put(type, result);
        }
        return result.get();
    }

    public BaseBuilder indent(int indent) {
        this.indent.addAndGet(indent);
        return this;
    }

    public BaseBuilder nl() {
        out.append("\n");
        return this;
    }

    public StringBuilder append(Object value) {
        indent();
        out.append(value);
        return out;
    }

    public BaseBuilder indent() {
        ws(indent.get());
        return this;
    }

    public BaseBuilder in() {
        indent(2);
        return this;
    }

    public BaseBuilder out() {
        indent(-2);
        return this;
    }

    public BaseBuilder ws(int n) {
        for (int i = 0; i < n; i++)
            out.append(" ");
        return this;
    }

    public BaseBuilder href(String label, URI target) {
        out.append("<a href=\"").append(target).append("\">").append(label).append("</a>");
        return this;
    }

    public FormBuilder form() {
        return new FormBuilder(this);
    }

    /** use form() */
    @Deprecated
    public BaseBuilder startForm(URI uri) {
        append("<form method=\"POST\" action=\"").append(uri).append("\">\n");
        in();
        return this;
    }

    /** use form() */
    @Deprecated
    public BaseBuilder endForm(String action, boolean autoHide) {
        if (autoHide)
            append("<noscript>\n  ");
        append("<input type=\"submit\" value=\"").append(action).append("\">\n");
        if (autoHide)
            append("</noscript>\n");
        out().append("</form>\n");
        return this;
    }

    public BaseBuilder close() {
        return container;
    }
}
