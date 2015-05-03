package com.github.t1.deployer.app.html.builder;

import java.net.URI;


public class Form extends HtmlBuilder {
    private Tag tag;

    public Form(HtmlBuilder container) {
        super(container);
        tag = tag("form");
    }

    public Form id(String id) {
        tag.id(id);
        return this;
    }

    public Form action(URI uri) {
        tag.attribute("method", "POST");
        tag.attribute("action", uri);
        return this;
    }

    @Override
    public StringBuilder append(Object value) {
        if (tag != null) {
            super.append(tag.header()).append("\n");
            in();
            tag = null;
        }
        return super.append(value);
    }

    public Form input(String label, String id) {
        return input(label, id, null);
    }

    public Form input(String label, String id, Object value) {
        return input(label, id, value, null);
    }

    public Form input(String label, String id, Object value, String placeholder) {
        append("<label for=\"").append(id).append("\">").append(label).append("</label>\n");
        Tag input = tag("input").classes("form-control").name(id).id(id);
        if (value != null)
            input.attribute("value", value);
        if (placeholder != null)
            input.attribute("placeholder", placeholder);
        input.attribute("required", null);
        input.close();
        append(input).append("\n");
        return this;
    }

    public Form hiddenInput(String name, Object value) {
        append("<input type=\"hidden\" name=\"").append(name).append("\" value=\"").append(value).append("\"/>\n");
        return this;
    }

    public Form noscriptSubmit(String label) {
        append("<noscript>\n  ");
        append("<input type=\"submit\" value=\"").append(label).append("\">\n");
        append("</noscript>\n");
        return this;
    }

    @Override
    public HtmlBuilder close() {
        out();
        append("</form>\n");
        return super.close();
    }

    public Form enclosing(HtmlBuilder body) {
        body.indent(indent());
        if (tag != null) {
            super.append(tag.header()).append("\n");
            in();
            tag = null;
        }
        rawAppend(body.close());
        return this;
    }
}
