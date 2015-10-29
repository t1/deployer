package com.github.t1.deployer.app.html.builder;

@FunctionalInterface
public interface Component {
    public static abstract class ComponentBuilder {
        public abstract Component build();
    }

    public abstract void writeTo(BuildContext context);

    public default void writeInlineTo(BuildContext context) {
        writeTo(context);
    }

    public default boolean isMultiLine() {
        return false;
    }
}
