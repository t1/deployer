package com.github.t1.deployer.app.html.builder;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import lombok.*;

public class BuildContext {
    private static final String SPACES = "                                                                          "
            + "                                                                                                     "
            + "                                                                                                     ";

    private final Map<Class<?>, Object> targets = new LinkedHashMap<>();

    private Writer out;
    private int indent;

    public BuildContext write(@NonNull Component component, @NonNull Writer out) {
        Writer origOut = this.out;
        try {
            this.out = out;
            component.writeTo(this);
            return this;
        } finally {
            this.out = origOut;
        }
    }

    public BuildContext append(Object object) {
        appendRaw(object.toString());
        return this;
    }

    @SneakyThrows(IOException.class)
    public BuildContext appendRaw(String string) {
        out.append(string);
        return this;
    }

    public BuildContext appendln(Object object) {
        append(object).appendln();
        return this;
    }

    public BuildContext appendln() {
        append("\n");
        return this;
    }

    public BuildContext print(CharSequence string) {
        return append(indent()).append(string);
    }

    public BuildContext println(CharSequence string) {
        return print(string).appendln();
    }

    private String indent() {
        return SPACES.substring(0, indent * 2);
    }

    public BuildContext in() {
        ++indent;
        return this;
    }

    public BuildContext out() {
        --indent;
        return this;
    }

    public BuildContext put(Object target) {
        this.targets.put(target.getClass(), target);
        return this;
    }

    // TODO get generic type, e.g. for List<T>

    public <T> T get(Class<T> type) {
        for (Entry<Class<?>, Object> entry : targets.entrySet())
            if (type.isAssignableFrom(entry.getKey()))
                return type.cast(entry.getValue());
        throw new IllegalStateException("no target for " + type);
    }
}
