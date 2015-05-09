package com.github.t1.deployer.app.html.builder2;

import static com.github.t1.deployer.app.html.builder2.Compound.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;

import java.util.*;

import lombok.*;

import com.github.t1.deployer.app.html.builder2.Compound.CompoundBuilder;

@Value
@Builder(builderMethodName = "tag")
@EqualsAndHashCode(callSuper = true)
public class Tag extends Component {
    private static final Component NO_VALUE = tag(UUID.randomUUID().toString()).build();

    public static TagBuilder tag(String name) {
        return new TagBuilder().name(name);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class Attribute extends Component {
        String key;
        Component value;

        @Override
        public void writeInlineTo(BuildContext out) {
            out.append(" ").append(key);
            if (value != NO_VALUE) {
                out.append("=\"");
                value.writeInlineTo(out);
                out.append("\"");
            }
        }

        @Override
        public void writeTo(BuildContext out) {
            writeInlineTo(out);
        }

        @Override
        public boolean isMultiLine() {
            return false;
        }
    }

    public static class TagBuilder {
        private String bodyDelimiter = "";
        private boolean multiline;

        public TagBuilder bodyDelimiter(String bodyDelimiter) {
            this.bodyDelimiter = bodyDelimiter;
            return this;
        }

        public TagBuilder a(String key, String value) {
            return a(key, text(value));
        }

        public TagBuilder a(String key) {
            return a(key, NO_VALUE);
        }

        public TagBuilder a(String key, Component value) {
            attribute(new Attribute(key, value));
            return this;
        }

        public TagBuilder id(String id) {
            return id(text(id));
        }

        public TagBuilder id(Component id) {
            return a("id", id);
        }

        public TagBuilder classes(String... classes) {
            for (String klass : classes) {
                classes(text(klass));
            }
            return this;
        }

        public TagBuilder classes(Component... classes) {
            CompoundBuilder compound = compound(" ");
            if (attributes != null) {
                for (Iterator<Attribute> iter = attributes.iterator(); iter.hasNext();) {
                    Attribute attribute = iter.next();
                    if (attribute.getKey().equals("class")) {
                        iter.remove();
                        compound.component(attribute.getValue());
                    }
                }
            }
            compound.component(classes);
            a("class", compound.build());
            return this;
        }

        public TagBuilder style(String style) {
            return a("style", style);
        }

        public TagBuilder body(Component body) {
            if (this.body == null)
                this.body = body;
            else
                this.body = compound(bodyDelimiter).component(this.body).component(body).build();
            this.multiline = this.multiline || this.body.isMultiLine();
            return this;
        }

        public TagBuilder multiline() {
            this.multiline = true;
            return this;
        }
    }

    @NonNull
    String name;
    @Singular
    List<Attribute> attributes;
    Component body;
    Boolean multiline;

    @Override
    public void writeTo(BuildContext out) {
        out.print("");
        writeInlineTo(out);
        out.appendln();
    }

    @Override
    public void writeInlineTo(BuildContext out) {
        out.append("<").append(name);
        if (attributes != null)
            for (Attribute attribute : attributes)
                attribute.writeInlineTo(out);
        if (body == null) {
            out.append("/>");
        } else if (body.isMultiLine()) {
            out.appendln(">").in();
            body.writeTo(out);
            out.out().print("</").append(name).append(">");
        } else {
            out.append(">");
            body.writeInlineTo(out);
            out.append("</").append(name).append(">");
        }
    }

    @Override
    public boolean isMultiLine() {
        return multiline;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        appendHeader(out);
        appendAttributes(out);
        appendBody(out);
        return out.toString();
    }

    private void appendHeader(StringBuilder out) {
        out.append(name);

        if (multiline)
            out.append(":multiline");
    }

    private void appendAttributes(StringBuilder out) {
        out.append("[");
        boolean first = true;
        for (Attribute attribute : attributes) {
            if (first)
                first = false;
            else
                out.append(",");
            out.append(attribute.getKey()).append(":").append(attribute.getValue());
        }
        out.append("]");
    }

    private void appendBody(StringBuilder out) {
        out.append("{");
        if (body != null)
            out.append(body);
        out.append("}");
    }
}
