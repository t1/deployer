package com.github.t1.deployer.app;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractHtmlWriter<T> extends HtmlWriter implements MessageBodyWriter<T> {
    private final Class<T> type;

    @Context
    UriInfo uriInfo;

    protected T target;
    protected StringBuilder out;

    public AbstractHtmlWriter() {
        this.type = null;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return this.type.equals(type);
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T target, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        log.debug("write as html: {}", target);
        uriInfo(uriInfo);
        this.out = new StringBuilder();
        this.target = target;
        Writer out = new OutputStreamWriter(entityStream);
        out.write(toString());
        out.flush();
    }

}
