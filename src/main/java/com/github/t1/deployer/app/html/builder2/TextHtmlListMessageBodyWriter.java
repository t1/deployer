package com.github.t1.deployer.app.html.builder2;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import javax.ws.rs.core.MediaType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class TextHtmlListMessageBodyWriter<T> extends TextHtmlMessageBodyWriter<List<T>> {
    /** Looks terrible, but it's actually not that bad: we just check if that genericType matches List<T> */
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (log.isTraceEnabled())
            log.trace("{}.isWriteable(type={}, generic={}, annotations={}, mediaType={})", getClass(), type,
                    genericType, Arrays.toString(annotations), mediaType);
        if (!List.class.isAssignableFrom(type))
            return false;
        if (!(genericType instanceof ParameterizedType))
            return false;
        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] typeArgs = parameterizedType.getActualTypeArguments();
        if (typeArgs.length != 1)
            return false;
        log.trace("{} is assignable from {}: {}", getType(), typeArgs[0],
                getType().isAssignableFrom((Class<?>) typeArgs[0]));
        return getType().isAssignableFrom((Class<?>) typeArgs[0]);
    }
}
