package com.github.t1.deployer.app.html.builder;

public class ButtonGroup extends HtmlBuilder {
    private boolean justified = false;

    public ButtonGroup(HtmlBuilder container) {
        super(container);
    }

    public ButtonGroup justified() {
        this.justified = true;
        return this;
    }

    @Override
    public Button button() {
        if (justified)
            appenButtonGroupDiv();
        // justified buttons need to be enclosed again: http://getbootstrap.com/components/#btn-groups-justified
        append("<div role=\"group\" class=\"btn-group\">\n");
        in();
        return super.button();
    }

    private void appenButtonGroupDiv() {
        Tag tag = div().attribute("role", "group").classes("btn-group").classes("btn-group-justified");
        append(tag.header()).append("\n");
        in();
    }

    @Override
    public HtmlBuilder close() {
        if (justified)
            out().append("</div>\n");
        out().append("</div>\n");
        return super.close();
    }
}
