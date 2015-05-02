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

import com.github.t1.deployer.app.html.builder.*;

@Slf4j
@Produces(TEXT_HTML)
public abstract class AbstractHtmlBodyWriter<T> extends TargetHtmlBuilder<T> implements MessageBodyWriter<T> {
    private final Class<T> type;
    private final Navigation activeNavigation;

    @Context
    public UriInfo uriInfo;

    public AbstractHtmlBodyWriter(Class<T> type, Navigation activeNavigation) {
        super(null);
        this.type = type;
        this.activeNavigation = activeNavigation;
    }

    public AbstractHtmlBodyWriter(BaseBuilder container, Navigation activeNavigation, Class<T> type, T target) {
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

    @Override
    public URI base(String path) {
        return uriInfo.getBaseUriBuilder().path(path).build();
    }

    @Override
    public void navigation() {
        for (Navigation navigation : Navigation.values()) {
            append("<li ");
            if (navigation == activeNavigation)
                out.append("class=\"active\"");
            out.append(">");
            href(navigation.title(), navigation.href(uriInfo));
            out.append("</li>\n");
        }
    }

    @Override
    public String title() {
        return activeNavigation.title();
    }
}
