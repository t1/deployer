package com.github.t1.deployer.app.html;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.List;

import javax.ws.rs.core.MediaType;

public abstract class AbstractListHtmlWriter<T> extends AbstractHtmlWriter<List<T>> {
    private final Class<T> type;

    /** @deprecated just required by weld */
    @Deprecated
    public AbstractListHtmlWriter() {
        super(null, null);
        this.type = null;
    }

    public AbstractListHtmlWriter(Class<T> type, Navigation active) {
        super(null, active);
        this.type = type;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return List.class.isAssignableFrom(type) //
                && genericType instanceof ParameterizedType //
                && ((ParameterizedType) genericType).getActualTypeArguments().length == 1 //
                && ((ParameterizedType) genericType).getActualTypeArguments()[0] instanceof Class //
                && this.type.isAssignableFrom((Class<?>) //
                        ((ParameterizedType) genericType).getActualTypeArguments()[0]);
    }
}
