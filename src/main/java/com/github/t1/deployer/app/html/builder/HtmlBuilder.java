package com.github.t1.deployer.app.html.builder;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class HtmlBuilder {
    private final HtmlBuilder container;

    private final StringBuilder out;
    private AtomicInteger indent;

    /** Constructor for single-line (i.e. non-indented), stand-alone components, e.g. normal anchor tags */
    public HtmlBuilder() {
        this.container = null;
        this.out = new StringBuilder();
        this.indent = new AtomicInteger();
    }

    /** Constructor for components directly contained in some other component */
    public HtmlBuilder(HtmlBuilder container) {
        this.container = container;
        this.indent = container.indent;
        this.out = container.out;
    }

    public void resetOutput() {
        this.out.setLength(0);
    }

    public HtmlBuilder indent(AtomicInteger indent) {
        this.indent = indent;
        return this;
    }

    public AtomicInteger indent() {
        return indent;
    }

    public HtmlBuilder nl() {
        out.append("\n");
        return this;
    }

    public StringBuilder append(Object value) {
        ws(indent.get());
        rawAppend(value);
        return out;
    }

    public StringBuilder rawAppend(Object value) {
        return out.append(value);
    }

    public HtmlBuilder in() {
        indent.getAndAdd(2);
        return this;
    }

    public HtmlBuilder out() {
        indent.getAndAdd(-2);
        return this;
    }

    public HtmlBuilder ws(int n) {
        for (int i = 0; i < n; i++)
            out.append(" ");
        return this;
    }

    public Tag href(URI href) {
        return new Tag("a").attribute("href", href);
    }

    public Tag href(String label, URI href) {
        return href(href).enclosing(label);
    }

    public Form form() {
        return new Form(this);
    }

    public Button button() {
        return new Button(this);
    }

    public ButtonGroup buttonGroup() {
        return new ButtonGroup(this);
    }

    public Tag div() {
        return new Tag("div");
    }

    public Tag span() {
        return new Tag("span");
    }

    public Tag listGroup() {
        return new Tag("ul").classes("list-group");
    }

    public Tag li() {
        return tag("li");
    }

    public Tag tag(String name) {
        return new Tag(name);
    }

    public HtmlBuilder close() {
        return (container == null) ? this : container;
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
