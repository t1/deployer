package com.github.t1.deployer;

import java.util.function.*;

import org.junit.rules.ExternalResource;

public class MementoRule<T> extends ExternalResource {
    private final Supplier<T> supplier;
    private final Consumer<T> consumer;
    private final T newValue;

    private T origValue;

    public MementoRule(Supplier<T> supplier, Consumer<T> consumer, T newValue) {
        this.supplier = supplier;
        this.consumer = consumer;
        this.newValue = newValue;
    }

    @Override
    public void before() {
        this.origValue = supplier.get();
        consumer.accept(newValue);
    }

    @Override
    public void after() {
        consumer.accept(origValue);
    }
}
