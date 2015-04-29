package com.github.t1.deployer.app.html;

import java.net.URI;

public class AbstractHtmlWriter<T> {
    protected T target;
    protected StringBuilder out;

    protected int indent = 0;

    public AbstractHtmlWriter<T> indent(int indent) {
        this.indent = indent;
        return this;
    }

    protected AbstractHtmlWriter<T> nl() {
        out.append("\n");
        return this;
    }

    protected StringBuilder append(Object value) {
        indent();
        out.append(value);
        return out;
    }

    protected AbstractHtmlWriter<T> indent() {
        ws(indent);
        return this;
    }

    protected AbstractHtmlWriter<T> in() {
        indent += 2;
        return this;
    }

    protected AbstractHtmlWriter<T> out() {
        indent -= 2;
        return this;
    }

    protected AbstractHtmlWriter<T> ws(int n) {
        for (int i = 0; i < n; i++)
            out.append(" ");
        return this;
    }

    protected AbstractHtmlWriter<T> href(String label, URI target) {
        out.append("<a href=\"").append(target).append("\">").append(label).append("</a>");
        return this;
    }

    protected AbstractHtmlWriter<T> startForm(URI uri) {
        append("<form method=\"POST\" action=\"").append(uri).append("\">\n");
        in();
        return this;
    }

    protected AbstractHtmlWriter<T> hiddenInput(String name, String value) {
        append("<input type=\"hidden\" name=\"").append(name).append("\" value=\"").append(value).append("\"/>\n");
        return this;
    }

    protected AbstractHtmlWriter<T> endForm(String action, boolean autoHide) {
        if (autoHide)
            append("<noscript>\n  ");
        append("<input type=\"submit\" value=\"").append(action).append("\">\n");
        if (autoHide)
            append("</noscript>\n");
        out().append("</form>\n");
        return this;
    }
}
