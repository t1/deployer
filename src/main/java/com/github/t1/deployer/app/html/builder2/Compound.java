package com.github.t1.deployer.app.html.builder2;

import static java.util.Collections.*;

import java.util.*;

import lombok.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class Compound extends Component {
    public static CompoundBuilder compound(Component... components) {
        return compound("", components);
    }

    public static CompoundBuilder compound(String delimiter, Component... components) {
        return new CompoundBuilder(delimiter).component(components);
    }

    @RequiredArgsConstructor
    public static class CompoundBuilder {
        private final String delimiter;
        private final List<Component> components = new ArrayList<>();

        public CompoundBuilder component(Component... components) {
            for (Component component : components)
                component(component);
            return this;
        }

        public CompoundBuilder component(Component component) {
            if (component instanceof Compound)
                for (Component sub : ((Compound) component).getComponents())
                    components.add(sub);
            else if (component != null)
                components.add(component);
            return this;
        }

        public Compound build() {
            return new Compound(delimiter, unmodifiableList(components));
        }
    }

    String delimiter;
    List<Component> components;

    @Override
    public void writeTo(BuildContext out) {
        boolean first = true;
        for (Component component : components) {
            if (first)
                first = false;
            else
                out.append(delimiter);
            component.writeTo(out);
        }
    }

    @Override
    public boolean isMultiLine() {
        if (components.size() > 1)
            return true;
        for (Component component : components)
            if (component.isMultiLine())
                return true;
        return false;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("[");
        boolean first = true;
        for (Component component : components) {
            if (first)
                first = false;
            else
                out.append(delimiter);
            out.append(component);
        }
        out.append("]");
        return out.toString();
    }
}
