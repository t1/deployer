package com.github.t1.deployer.app.html.builder2;

import java.io.*;

import lombok.*;

@RequiredArgsConstructor
public class BuildContext {
    private static final String SPACES = "                                                                          "
            + "                                                                                                     ";

    @NonNull
    private final Component component;
    private final Object target;

    private Writer out;
    private int indent;

    public void to(Writer out) {
        this.out = out;
        component.writeTo(this);
    }

    @SneakyThrows(IOException.class)
    public BuildContext append(Object object) {
        out.append(object.toString());
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

    @SuppressWarnings("unchecked")
    public <T> T getTarget() {
        return (T) target;
    }
}
