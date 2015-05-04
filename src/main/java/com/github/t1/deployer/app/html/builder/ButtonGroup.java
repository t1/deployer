package com.github.t1.deployer.app.html.builder;

public class ButtonGroup extends HtmlBuilder {
    private boolean justified = false;
    private boolean started = false;

    public ButtonGroup(HtmlBuilder container) {
        super(container);
    }

    public ButtonGroup justified() {
        this.justified = true;
        return this;
    }

    @Override
    public Button button() {
        if (started) {
            closeButtonDiv();
        } else {
            started = true;
            if (justified) {
                appenButtonGroupDiv("btn-group-justified");
            } else {
                appenButtonGroupDiv();
            }
        }

        // justified buttons need to be enclosed again: http://getbootstrap.com/components/#btn-groups-justified
        if (justified)
            appenButtonGroupDiv();
        return super.button();
    }

    private void appenButtonGroupDiv(String... classes) {
        Tag tag = div().attribute("role", "group").classes("btn-group").classes(classes);
        append(tag.header()).append("\n");
        in();
    }

    @Override
    public HtmlBuilder close() {
        if (justified)
            out().append("</div>\n");
        closeButtonDiv();
        return super.close();
    }

    private void closeButtonDiv() {
        out().append("</div>\n");
    }
}
