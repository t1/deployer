package com.github.t1.deployer.app.html.builder;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.security.Principal;

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Produces(TEXT_HTML)
public abstract class TextHtmlMessageBodyWriter<T> implements MessageBodyWriter<T> {
    @Context
    UriInfo uriInfo;
    @Inject
    Principal principal;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean textHtml = isTextHtml(mediaType);
        Class<?> writerType = getType();
        boolean assignable = writerType.isAssignableFrom(type);
        log.trace("isWriteable: type {}, genericType {}, mediaType {}, writerType {} -> html: {}, assignable: {}", type,
                genericType, mediaType, writerType, textHtml, assignable);
        return textHtml && assignable;
    }

    private boolean isTextHtml(MediaType mediaType) {
        return (TEXT_HTML_TYPE.isCompatible(mediaType) || APPLICATION_XHTML_XML_TYPE.isCompatible(mediaType))
                && !WILDCARD_TYPE.equals(mediaType);
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
        httpHeaders.add("X-Frame-Options", "DENY");
        httpHeaders.add("X-XSS-Protection", "1; mode=block");
        httpHeaders.add("X-Content-Type-Options", "nosniff");
        try {
            OutputStreamWriter out = new OutputStreamWriter(entityStream);
            BuildContext context = new BuildContext();
            context.put(target).put(uriInfo);
            if (principal != null)
                context.put(principal);
            prepare(context);
            context.write(component(), out);
            out.flush();
        } catch (Exception e) {
            log.error("failed to write " + genericType + " as " + mediaType, e);
            throw e;
        }
    }

    protected void prepare(@SuppressWarnings("unused") BuildContext buildContext) {}

    protected abstract Component component();
}
