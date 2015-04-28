package com.github.t1.deployer.app.html;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Produces(TEXT_HTML)
@RequiredArgsConstructor
public abstract class AbstractHtmlWriter<T> implements MessageBodyWriter<T> {
    private static final String MIN = ""; // ".min";

    private final Class<T> type;
    private final Navigation active;

    @Context
    UriInfo uriInfo;

    protected T target;
    private StringBuilder out;

    /** @deprecated just required by weld */
    @Deprecated
    public AbstractHtmlWriter() {
        this.type = null;
        this.active = null;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return this.type.isAssignableFrom(type);
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T target, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        log.debug("write as html: {} for {}", target, uriInfo.getRequestUri());
        this.target = target;
        this.out = new StringBuilder();

        html();

        new OutputStreamWriter(entityStream).append(out.toString()).flush();
    }

    protected URI base(String path) {
        return uriInfo.getBaseUriBuilder().path(path).build();
    }

    protected AbstractHtmlWriter<T> nl() {
        out.append("\n");
        return this;
    }

    protected void html() {
        append("<!DOCTYPE html>\n");
        append("<html>\n"); // lang="en"
        head();
        append("  <body class=\"container\">\n");
        navBar();
        append("  <div class=\"jumbotron\">\n");
        append("    <h1>").append(bodyTitle()).append("</h1>\n");
        nl();
        body();
        nl();
        append("    ").append(script("jquery/jquery" + MIN + ".js")).append("\n");
        append("    ").append(script("bootstrap/js/bootstrap" + MIN + ".js")).append("\n");
        append("  </div>\n");
        append("  </body>\n");
        append("</html>\n");
    }

    protected void head() {
        append("  <head>\n");
        append("    <meta charset=\"utf-8\">\n");
        append("    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");
        append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        append("    <title>").append(headerTitle()).append("</title>\n");
        nl();
        append("    ").append(stylesheet("bootstrap/css/bootstrap" + MIN + ".css")).append("\n");
        append("    ").append(stylesheet("webapp/css/style.css")).append("\n");
        append("  </head>\n");
    }

    protected void navBar() {
        append("      <nav class=\"navbar navbar-default\">\n");
        append("        <div class=\"container-fluid\">\n");
        append("          <div class=\"navbar-header\">\n");
        append("            <a class=\"navbar-brand\" href=\"#\">Deployer</a>\n");
        append("          </div>\n");
        append("          <div id=\"navbar\" class=\"navbar-collapse collapse\">\n");
        append("            <ul class=\"nav navbar-nav navbar-right\">\n");
        for (Navigation navigation : Navigation.values()) {
            append("              <li ");
            if (navigation == active)
                append("class=\"active\"");
            append(">");
            href(navigation.title(), navigation.href(uriInfo));
            append("</li>\n");
        }
        append("            </ul>\n");
        append("          </div>\n");
        append("        </div>\n");
        append("      </nav>\n");
        append("\n");
    }

    protected String headerTitle() {
        return title();
    }

    protected String bodyTitle() {
        return title();
    }

    protected abstract String title();

    protected abstract void body();

    private int indent = 0;

    protected StringBuilder append(Object value) {
        ws(indent);
        out.append(value);
        return out;
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
        append("<a href=\"").append(target).append("\">").append(label).append("</a>");
        return this;
    }

    protected void br() {
        append("<br/><br/>\n");
    }

    private String stylesheet(String path) {
        return "<link href=\"" + base(path) + "\" rel=\"stylesheet\"/>";
    }

    private String script(String path) {
        return "<script src=\"" + base(path) + "\"/>";
    }
}
