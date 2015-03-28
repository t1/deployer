package com.github.t1.deployer.app;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;

public abstract class AbstractListHtmlWriter<T> extends AbstractHtmlWriter<List<T>> {
    private final Class<T> type;

    public AbstractListHtmlWriter() {
        super(null);
        this.type = null;
    }

    public AbstractListHtmlWriter(Class<T> type) {
        super(null);
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

    protected void link(String label, URI target) {
        out.append("<a href=\"" + target + "\">" + label + "</a>");
    }

    protected void br() {
        out.append("<br/><br/>\n");
    }
}
