package com.github.t1.deployer.app.html.builder2;

import static javax.ws.rs.core.MediaType.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(TEXT_HTML)
public abstract class TextHtmlListMessageBodyWriter<T> extends TextHtmlMessageBodyWriter<List<T>> {
    /** Looks terrible, but it's actually not that bad: we just check if that genericType matches List<T> */
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (!List.class.isAssignableFrom(type))
            return false;
        if (!(genericType instanceof ParameterizedType))
            return false;
        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] typeArgs = parameterizedType.getActualTypeArguments();
        if (typeArgs.length != 1)
            return false;
        return getType().isAssignableFrom((Class<?>) typeArgs[0]);
    }
}
