package com.github.t1.deployer.app.html.builder;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseBuilder {
    private final BaseBuilder container;

    private final StringBuilder out;
    private final AtomicInteger indent;

    public BaseBuilder() {
        this.container = null;
        this.out = new StringBuilder();
        this.indent = new AtomicInteger();
    }

    public BaseBuilder(BaseBuilder container) {
        this.container = container;
        this.indent = container.indent;
        this.out = container.out;
    }

    public void resetOutput() {
        this.out.setLength(0);
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
        rawAppend(value);
        return out;
    }

    public BaseBuilder indent() {
        ws(indent.get());
        return this;
    }

    public StringBuilder rawAppend(Object value) {
        return out.append(value);
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

    public TagBuilder href(URI href) {
        return new TagBuilder("a").attribute("href", href);
    }

    public BaseBuilder href(String label, URI target) {
        out.append("<a href=\"").append(target).append("\">").append(label).append("</a>");
        return this;
    }

    public FormBuilder form() {
        return new FormBuilder(this);
    }

    public TagBuilder li() {
        return tag("li");
    }

    public TagBuilder tag(String name) {
        return new TagBuilder(name);
    }

    public BaseBuilder close() {
        return container;
    }

    public BaseBuilder closing(BaseBuilder body) {
        return body.close();
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
