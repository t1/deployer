package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.StyleVariation.*;

import java.net.URI;


public class FormBuilder extends HtmlBuilder {
    public FormBuilder(HtmlBuilder htmlWriter) {
        super(htmlWriter);
    }

    public FormBuilder action(URI uri) {
        append("<form method=\"POST\" action=\"").append(uri).append("\">\n");
        in();
        return this;
    }

    protected FormBuilder input(String label, String id) {
        return input(label, id, null);
    }

    protected FormBuilder input(String label, String id, Object value) {
        return input(label, id, value, null);
    }

    protected FormBuilder input(String label, String id, Object value, String placeholder) {
        append("<label for=\"").append(id).append("\">").append(label).append("</label>\n");
        append("<input class=\"form-control\" name=\"").append(id).append(" id=\"").append(id);
        if (value != null)
            out.append("\" value=\"").append(value);
        if (placeholder != null)
            out.append("\" placeholder=\"").append(placeholder);
        out.append("\" required/>\n");
        return this;
    }

    protected FormBuilder hiddenInput(String name, String value) {
        append("<input type=\"hidden\" name=\"").append(name).append("\" value=\"").append(value).append("\"/>\n");
        return this;
    }

    protected FormBuilder noscriptSubmit(String label) {
        append("<noscript>\n  ");
        append("<input type=\"submit\" value=\"").append(label).append("\">\n");
        append("</noscript>\n");
        return this;
    }

    protected FormBuilder submit(String label) {
        return submit(label, primary);
    }

    protected FormBuilder submit(String label, StyleVariation variation) {
        append("<div class=\"btn-group btn-group-justified\" role=\"group\">\n");
        append("  <div class=\"btn-group\" role=\"group\">\n");
        append("    <button class=\"btn btn-lg btn-").append(variation).append(" btn-block\" type=\"submit\">") //
                .append(label) //
                .append("</button>\n");
        append("  </div>\n");
        append("</div>\n");
        return this;
    }

    public HtmlBuilder submitIcon(String icon, StyleVariation variation) {
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
    protected HtmlBuilder close() {
        out();
        append("</form>\n");
        return super.close();
    }
}
