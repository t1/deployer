package com.github.t1.deployer.app.html;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import com.github.t1.deployer.app.html.builder.*;

@Slf4j
@Produces(TEXT_HTML)
public abstract class AbstractHtmlBodyWriter<T> extends TargetPage<T> implements MessageBodyWriter<T> {
    private final Class<T> type;
    private final Navigation activeNavigation;

    @Context
    @Getter
    private UriInfo uriInfo;

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
        this.setTarget(target);
        resetOutput();

        html();

        new OutputStreamWriter(entityStream).append(toString()).flush();
    }

    @Override
    public URI base(String path) {
        return uriInfo.getBaseUriBuilder().path(path).build();
    }

    @Override
    public void navigation() {
        for (Navigation navigation : Navigation.values()) {
            Tag li = li();
            if (navigation == activeNavigation)
                li.classes("active");
            URI uri = navigation.href(uriInfo);
            li.enclosing(href(uri).enclosing(navigation.title()));
            append(li).append("\n");
        }
    }

    @Override
    public String title() {
        return activeNavigation.title();
    }
}
