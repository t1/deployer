package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.StyleVariation.*;

import java.net.URI;


public class FormBuilder extends BaseBuilder {
    public FormBuilder(BaseBuilder container) {
        super(container);
    }

    public FormBuilder action(URI uri) {
        append("<form method=\"POST\" action=\"").append(uri).append("\">\n");
        in();
        return this;
    }

    public FormBuilder input(String label, String id) {
        return input(label, id, null);
    }

    public FormBuilder input(String label, String id, Object value) {
        return input(label, id, value, null);
    }

    public FormBuilder input(String label, String id, Object value, String placeholder) {
        append("<label for=\"").append(id).append("\">").append(label).append("</label>\n");
        TagBuilder input = tag("input").classes("form-control").name(id).id(id);
        if (value != null)
            input.attribute("value", value);
        if (placeholder != null)
            input.attribute("placeholder", placeholder);
        input.attribute("required", null);
        input.close();
        append(input).append("\n");
        return this;
    }

    public FormBuilder hiddenInput(String name, Object value) {
        append("<input type=\"hidden\" name=\"").append(name).append("\" value=\"").append(value).append("\"/>\n");
        return this;
    }

    public FormBuilder noscriptSubmit(String label) {
        append("<noscript>\n  ");
        append("<input type=\"submit\" value=\"").append(label).append("\">\n");
        append("</noscript>\n");
        return this;
    }

    public FormBuilder submit(String label) {
        return submit(label, primary);
    }

    public FormBuilder submit(String label, StyleVariation variation) {
        append("<div class=\"btn-group btn-group-justified\" role=\"group\">\n");
        append("  <div class=\"btn-group\" role=\"group\">\n");
        append("    <button class=\"btn btn-lg btn-").append(variation).append(" btn-block\" type=\"submit\">") //
                .append(label) //
                .append("</button>\n");
        append("  </div>\n");
        append("</div>\n");
        return this;
    }

    public BaseBuilder submitIcon(String icon, StyleVariation variation) {
        append("<div class=\"btn-group btn-group-justified\" role=\"group\">\n");
        append("  <div class=\"btn-group\" role=\"group\">\n");
        append("    <button class=\"btn btn-lg btn-").append(variation).append(" btn-block\" type=\"submit\">\n");
        append("      <span class=\"glyphicon glyphicon-").append(icon).append("\"></span>\n");
        append("    </button>\n");
        append("  </div>\n");
        append("</div>\n");
        return this;
    }

    @Override
    public BaseBuilder close() {
        out();
        append("</form>\n");
        return super.close();
    }

    @Override
    public FormBuilder closing(BaseBuilder sub) {
        sub.close();
        return this;
    }
}
