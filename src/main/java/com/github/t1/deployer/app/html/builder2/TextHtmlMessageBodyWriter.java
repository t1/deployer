package com.github.t1.deployer.app.html.builder2;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;

import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

@Provider
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
        OutputStreamWriter writer = new OutputStreamWriter(entityStream);
        component().write(target, uriInfo).to(writer);
        writer.flush();
    }

    protected abstract Component component();
}
