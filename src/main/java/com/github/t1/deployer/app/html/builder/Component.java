package com.github.t1.deployer.app.html.builder;

@FunctionalInterface
public interface Component {
    public static abstract class ComponentBuilder {
        public abstract Component build();
    }

    public abstract void writeTo(BuildContext out);

    public default void writeInlineTo(BuildContext out) {
        writeTo(out);
    }

    public default boolean isMultiLine() {
        return false;
    }
}
