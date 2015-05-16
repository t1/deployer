package com.github.t1.deployer.app.html.builder;

public abstract class Component {
    public static abstract class ComponentBuilder {
        public abstract Component build();
    }

    public BuildContext write(Object... targets) {
        return new BuildContext(this, targets);
    }

    public abstract void writeTo(BuildContext out);

    public void writeInlineTo(BuildContext out) {
        writeTo(out);
    }

    public boolean isMultiLine() {
        return false;
    }
}
