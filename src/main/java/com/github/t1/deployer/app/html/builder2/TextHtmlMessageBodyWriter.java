package com.github.t1.deployer.app.html.builder2;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;

import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Produces(TEXT_HTML)
public abstract class TextHtmlMessageBodyWriter<T> implements MessageBodyWriter<T> {
    @Context
    UriInfo uriInfo;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return getType().isAssignableFrom(type);
    }

    protected Class<?> getType() {
        Type generic = getClass().getGenericSuperclass();
        if (generic instanceof Class)
            return (Class<?>) generic;
        return (Class<?>) ((ParameterizedType) generic).getActualTypeArguments()[0];
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T target, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(entityStream);
            BuildContext buildContext = component().write(target, uriInfo);
            prepare(buildContext);
            buildContext.to(writer);
            writer.flush();
        } catch (Exception e) {
            log.error("failed to write " + genericType + " as " + mediaType, e);
            throw e;
        }
    }

    protected void prepare(@SuppressWarnings("unused") BuildContext buildContext) {}

    protected abstract Component component();
}
