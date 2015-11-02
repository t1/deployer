package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Compound.*;
import static com.github.t1.deployer.app.html.builder.Static.*;

import java.util.*;
import java.util.Map.Entry;

import lombok.*;

@Value
@Builder(builderMethodName = "tag")
public class Tag implements Component {
    public static TagBuilder tag(String name) {
        return new TagBuilder().name(name);
    }

    @Value
    @AllArgsConstructor
    public static class Attribute {
        @NonNull
        Component key;

        Component value;

        public Attribute(String key, Component value) {
            this(text(key), value);
        }
    }

    public static class TagBuilder implements ComponentBuilder {
        private String bodyDelimiter = "";
        private boolean multiline;

        public TagBuilder bodyDelimiter(String bodyDelimiter) {
            this.bodyDelimiter = bodyDelimiter;
            return this;
        }

        public TagBuilder attr(String key, String value) {
            return attr(key, text(value));
        }

        public TagBuilder attr(String key) {
            return attr(key, (Component) null);
        }

        public TagBuilder attr(Component key) {
            return attr(key, (Component) null);
        }

        public TagBuilder attr(String key, Component value) {
            attribute(new Attribute(key, value));
            return this;
        }

        public TagBuilder attr(Component key, Component value) {
            attribute(new Attribute(key, value));
            return this;
        }

        public TagBuilder id(String id) {
            return id(text(id));
        }

        public TagBuilder id(Component id) {
            return attr("id", id);
        }

        public TagBuilder classes(String... classes) {
            for (String klass : classes)
                classes(text(klass));
            return this;
        }

        public TagBuilder classes(Component... classes) {
            attr("class", compound(" ", classes).build());
            return this;
        }

        public TagBuilder style(String style) {
            return attr("style", style);
        }

        public TagBuilder body(ComponentBuilder body) {
            return this.body(body.build());
        }

        public TagBuilder body(Component body) {
            if (this.body == null)
                this.body = body;
            else
                this.body = compound(bodyDelimiter, this.body, body).build();
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
            writeAttributes(out);
        if (body == null)
            out.append("/>");
        else if (body.isMultiLine()) {
            out.appendln(">").in();
            body.writeTo(out);
            out.out().print("</").append(name).append(">");
        } else {
            out.append(">");
            body.writeInlineTo(out);
            out.append("</").append(name).append(">");
        }
    }

    private void writeAttributes(BuildContext context) {
        for (Entry<String, String> entry : getAttributeMap(context).entrySet()) {
            context.append(" ").append(entry.getKey());
            if (entry.getValue() != null) {
                context.append("=\"").append(entry.getValue()).append("\"");
            }
        }
    }

    /**
     * Build all attributes to a <code>Map<String,String></code>, concatenating attributes with the same name with a
     * space as delimiter. This is required namely for class attributes.
     */
    public Map<String, String> getAttributeMap(BuildContext context) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Attribute attribute : attributes) {
            String key = attribute.getKey().asString(context);
            if (key.isEmpty()) // optional tag
                continue;
            String value = (attribute.getValue() == null) ? null : attribute.getValue().asString(context);
            if (map.containsKey(key))
                value = map.get(key) + " " + value;
            map.put(key, value);
        }
        return map;
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
