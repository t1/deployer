package com.github.t1.deployer.model;

import java.util.Optional;
import java.util.function.Supplier;

import lombok.Value;

@Value
public class Property<T> {
    private final Supplier<Optional<T>> supplier;

    public Optional<T> get() {
        return supplier.get();
    }
}
