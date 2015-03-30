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

    @Context
    UriInfo uriInfo;

    protected T target;
    protected StringBuilder out;

    /** @deprecated this seems to be required by jax-rs */
    @Deprecated
    public AbstractHtmlWriter() {
        this.type = null;
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

    protected void html() {
        out.append("<!DOCTYPE html>\n");
        out.append("<html>\n"); // lang="en"
        head();
        out.append("  <body>\n");
        out.append("    <h1>").append(title()).append("</h1>\n");
        out.append("\n");
        body();
        out.append("\n");
        out.append("    ").append(script("jquery/jquery.min.js")).append("\n");
        out.append("    ").append(script("bootstrap/js/bootstrap.min.js")).append("\n");
        out.append("  </body>\n");
        out.append("</html>\n");
    }

    private void head() {
        out.append("  <head>\n");
        out.append("    <meta charset=\"utf-8\">\n");
        out.append("    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");
        out.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        out.append("    <title>").append(title()).append("</title>\n");
        out.append("\n");
        out.append("    ").append(stylesheet("bootstrap/css/bootstrap.min.css")).append("\n");
        out.append("    ").append(stylesheet("webapp/css/style.css")).append("\n");
        out.append("  </head>\n");
    }

    protected abstract String title();

    protected abstract void body();

    private String stylesheet(String path) {
        return "<link href=\"" + base(path) + "\" rel=\"stylesheet\"/>";
    }

    private String script(String path) {
        return "<script src=\"" + base(path) + "\"/>";
    }
}
