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
    private final Class<T> type;
    private final Navigation active;

    @Context
    UriInfo uriInfo;

    protected T target;
    protected StringBuilder out;

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

    protected void nl() {
        out.append("\n");
    }

    protected void html() {
        out.append("<!DOCTYPE html>\n");
        out.append("<html>\n"); // lang="en"
        head();
        out.append("  <body class=\"container\">\n");
        navBar();
        out.append("  <div class=\"jumbotron\">\n");
        out.append("    <h1>").append(bodyTitle()).append("</h1>\n");
        nl();
        body();
        nl();
        out.append("    ").append(script("jquery/jquery.min.js")).append("\n");
        out.append("    ").append(script("bootstrap/js/bootstrap.min.js")).append("\n");
        out.append("  </div>\n");
        out.append("  </body>\n");
        out.append("</html>\n");
    }

    protected void head() {
        out.append("  <head>\n");
        out.append("    <meta charset=\"utf-8\">\n");
        out.append("    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");
        out.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        out.append("    <title>").append(headerTitle()).append("</title>\n");
        nl();
        out.append("    ").append(stylesheet("bootstrap/css/bootstrap.min.css")).append("\n");
        out.append("    ").append(stylesheet("webapp/css/style.css")).append("\n");
        out.append("  </head>\n");
    }

    protected void navBar() {
        out.append("      <nav class=\"navbar navbar-default\">\n");
        out.append("        <div class=\"container-fluid\">\n");
        out.append("          <div class=\"navbar-header\">\n");
        out.append("            <a class=\"navbar-brand\" href=\"#\">Deployer</a>\n");
        out.append("          </div>\n");
        out.append("          <div id=\"navbar\" class=\"navbar-collapse collapse\">\n");
        out.append("            <ul class=\"nav navbar-nav navbar-right\">\n");
        for (Navigation navigation : Navigation.values()) {
            out.append("              <li ");
            if (navigation == active)
                out.append("class=\"active\"");
            out.append(">");
            href(navigation.title(), navigation.href(uriInfo));
            out.append("</li>\n");
        }
        out.append("            </ul>\n");
        out.append("          </div>\n");
        out.append("        </div>\n");
        out.append("      </nav>\n");
        out.append("\n");
    }

    protected String headerTitle() {
        return title();
    }

    protected String bodyTitle() {
        return title();
    }

    protected abstract String title();

    protected abstract void body();

    protected void href(String label, URI target) {
        out.append("<a href=\"" + target + "\">" + label + "</a>");
    }

    protected void br() {
        out.append("<br/><br/>\n");
    }

    private String stylesheet(String path) {
        return "<link href=\"" + base(path) + "\" rel=\"stylesheet\"/>";
    }

    private String script(String path) {
        return "<script src=\"" + base(path) + "\"/>";
    }
}
