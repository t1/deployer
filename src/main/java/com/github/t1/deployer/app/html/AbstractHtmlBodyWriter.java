package com.github.t1.deployer.app.html;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Produces(TEXT_HTML)
public abstract class AbstractHtmlBodyWriter<T> extends AbstractHtmlWriter<T> implements MessageBodyWriter<T> {
    private static final String MIN = ""; // ".min";

    private final Class<T> type;
    private final Navigation activeNavigation;

    @Context
    UriInfo uriInfo;

    public AbstractHtmlBodyWriter(Class<T> type, Navigation activeNavigation) {
        super(null);
        this.type = type;
        this.activeNavigation = activeNavigation;
    }

    public AbstractHtmlBodyWriter(HtmlBuilder container, Navigation activeNavigation, Class<T> type, T target) {
        super(container, target);
        this.type = type;
        this.activeNavigation = activeNavigation;
    }

    /** @deprecated just required by weld */
    @Deprecated
    public AbstractHtmlBodyWriter() {
        super(null, null);
        this.type = null;
        this.activeNavigation = null;
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
        this.out.setLength(0);

        html();

        new OutputStreamWriter(entityStream).append(out.toString()).flush();
    }

    protected URI base(String path) {
        return uriInfo.getBaseUriBuilder().path(path).build();
    }

    @Override
    protected AbstractHtmlBodyWriter<T> nl() {
        out.append("\n");
        return this;
    }

    protected void html() {
        append("<!DOCTYPE html>\n");
        append("<html>\n"); // lang="en"
        in();
        head();
        append("<body class=\"container\">\n");
        in();
        navBar();
        append("<div class=\"jumbotron\">\n");
        in();
        append("<h1>").append(bodyTitle()).append("</h1>\n");
        nl();
        body();
        nl();
        append(script("jquery/jquery" + MIN + ".js")).append("\n");
        append(script("bootstrap/js/bootstrap" + MIN + ".js")).append("\n");
        out();
        append("</div>\n");
        out();
        append("</body>\n");
        out();
        append("</html>\n");
    }

    protected void head() {
        append("<head>\n");
        in();
        append("<meta charset=\"utf-8\">\n");
        append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        append("<title>").append(headerTitle()).append("</title>\n");
        nl();
        append(stylesheet("bootstrap/css/bootstrap" + MIN + ".css")).append("\n");
        append(stylesheet("webapp/css/style.css")).append("\n");
        out();
        append("</head>\n");
    }

    protected void navBar() {
        append("<nav class=\"navbar navbar-default\">\n");
        in();
        append("<div class=\"container-fluid\">\n");
        in();
        append("<div class=\"navbar-header\">\n");
        append("  <a class=\"navbar-brand\" href=\"#\">Deployer</a>\n");
        append("</div>\n");
        append("<div id=\"navbar\" class=\"navbar-collapse collapse\">\n");
        in();
        append("<ul class=\"nav navbar-nav navbar-right\">\n");
        in();
        for (Navigation navigation : Navigation.values()) {
            append("<li ");
            if (navigation == activeNavigation)
                out.append("class=\"active\"");
            out.append(">");
            href(navigation.title(), navigation.href(uriInfo));
            out.append("</li>\n");
        }
        out();
        append("</ul>\n");
        out();
        append("</div>\n");
        out();
        append("</div>\n");
        out();
        append("</nav>\n");
        nl();
    }

    protected String headerTitle() {
        return title();
    }

    protected String bodyTitle() {
        return title();
    }

    protected String title() {
        return activeNavigation.title();
    }

    protected abstract void body();

    private String stylesheet(String path) {
        return "<link href=\"" + base(path) + "\" rel=\"stylesheet\"/>";
    }

    private String script(String path) {
        return "<script src=\"" + base(path) + "\"/>";
    }
}
