package com.github.t1.deployer.app.html;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HtmlBuilder {
    private final HtmlBuilder container;

    private final Map<String, AtomicInteger> ids;
    protected final StringBuilder out;
    protected final AtomicInteger indent;

    public HtmlBuilder() {
        this.container = null;
        this.ids = new HashMap<>();
        this.out = new StringBuilder();
        this.indent = new AtomicInteger();
    }

    public HtmlBuilder(HtmlBuilder container) {
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

    public HtmlBuilder indent(int indent) {
        this.indent.addAndGet(indent);
        return this;
    }

    protected HtmlBuilder nl() {
        out.append("\n");
        return this;
    }

    protected StringBuilder append(Object value) {
        indent();
        out.append(value);
        return out;
    }

    protected HtmlBuilder indent() {
        ws(indent.get());
        return this;
    }

    protected HtmlBuilder in() {
        indent(2);
        return this;
    }

    protected HtmlBuilder out() {
        indent(-2);
        return this;
    }

    protected HtmlBuilder ws(int n) {
        for (int i = 0; i < n; i++)
            out.append(" ");
        return this;
    }

    protected HtmlBuilder href(String label, URI target) {
        out.append("<a href=\"").append(target).append("\">").append(label).append("</a>");
        return this;
    }

    protected FormBuilder form() {
        return new FormBuilder(this);
    }

    /** use form() */
    @Deprecated
    protected HtmlBuilder startForm(URI uri) {
        append("<form method=\"POST\" action=\"").append(uri).append("\">\n");
        in();
        return this;
    }

    /** use form() */
    @Deprecated
    protected HtmlBuilder endForm(String action, boolean autoHide) {
        if (autoHide)
            append("<noscript>\n  ");
        append("<input type=\"submit\" value=\"").append(action).append("\">\n");
        if (autoHide)
            append("</noscript>\n");
        out().append("</form>\n");
        return this;
    }

    protected HtmlBuilder close() {
        return container;
    }
}
