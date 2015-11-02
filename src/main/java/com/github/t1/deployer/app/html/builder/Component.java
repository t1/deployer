package com.github.t1.deployer.app.html.builder;

import java.io.StringWriter;

@FunctionalInterface
public interface Component {
    public interface ComponentBuilder {
        public abstract Component build();
    }

    public abstract void writeTo(BuildContext context);

    public default void writeInlineTo(BuildContext context) {
        writeTo(context);
    }

    public default String asString(BuildContext context) {
        StringWriter out = new StringWriter();
        context.write(this, out);
        return out.toString();
    }

    public default boolean isMultiLine() {
        return false;
    }
}
