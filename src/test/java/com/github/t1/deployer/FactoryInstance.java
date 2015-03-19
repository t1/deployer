package com.github.t1.deployer;

import static java.util.Collections.*;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import javax.enterprise.inject.Instance;

import lombok.AllArgsConstructor;

import org.glassfish.hk2.api.Factory;
import org.jboss.weld.exceptions.UnsupportedOperationException;

@AllArgsConstructor
public class FactoryInstance<T> implements Instance<T> {
    private final Factory<T> factory;

    @Override
    public Iterator<T> iterator() {
        return singleton(get()).iterator();
    }

    @Override
    public T get() {
        return factory.provide();
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(javax.enterprise.util.TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUnsatisfied() {
        return false;
    }

    @Override
    public boolean isAmbiguous() {
        return false;
    }

    @Override
    public void destroy(T instance) {
        factory.dispose(instance);
    }
}
