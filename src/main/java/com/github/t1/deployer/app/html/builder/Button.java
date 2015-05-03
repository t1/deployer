package com.github.t1.deployer.app.html.builder;

public class Button extends HtmlBuilder {
    private final Tag tag = new Tag("button").classes("btn", "btn-block");

    public Button(HtmlBuilder container) {
        super(container);
    }

    public Button form(String formId) {
        tag.attribute("form", formId);
        return this;
    }

    public Button type(String type) {
        tag.attribute("type", type);
        return this;
    }

    public Button size(SizeVariation size) {
        tag.classes("btn" + size.suffix);
        return this;
    }

    public Button style(StyleVariation style) {
        tag.classes("btn-" + style);
        return this;
    }

    public Button icon(String name) {
        append(tag.header()).append("\n");
        in();
        append(span().classes("glyphicon", "glyphicon-" + name).close()).append("\n");
        out();
        append("</button>\n");
        return this;
    }

    public Button label(String label) {
        tag.enclosing(label);
        append(tag).append("\n");
        return this;
    }
}
