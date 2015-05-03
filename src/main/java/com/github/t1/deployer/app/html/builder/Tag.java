package com.github.t1.deployer.app.html.builder;

import java.util.*;

public class Tag extends HtmlBuilder {
    private final String tagName;
    private Map<String, String> attributes;
    private boolean headerClosed = false;

    public Tag(String tagName) {
        super(); // explicitly no container
        this.tagName = tagName;
    }

    public String attribute(String name) {
        return (attributes == null) ? null : attributes.get(name);
    }

    public Tag attribute(String name, Object value) {
        if (attributes == null)
            attributes = new LinkedHashMap<>();
        attributes.put(name, value == null ? null : value.toString());
        return this;
    }

    public Tag classes(String... strings) {
        StringBuilder out = new StringBuilder();
        String existingClasses = attribute("class");
        if (existingClasses != null)
            out.append(existingClasses);
        for (String string : strings) {
            if (out.length() > 0)
                out.append(" ");
            out.append(string);
        }
        attribute("class", out.toString());
        return this;
    }

    public Tag name(String name) {
        return attribute("name", name);
    }

    public Tag id(String id) {
        return attribute("id", id);
    }

    @Override
    public HtmlBuilder close() {
        closeHeader();
        rawAppend("/>");
        return super.close();
    }

    public Tag enclosing(Object body) {
        closeHeader();
        rawAppend(">");
        append(body);
        rawAppend("</").append(tagName).append(">");
        return this;
    }

    private void closeHeader() {
        if (headerClosed)
            return;
        headerClosed = true;
        super.append("<").append(tagName);
        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                rawAppend(" ").append(entry.getKey());
                if (entry.getValue() != null)
                    rawAppend("=\"").append(entry.getValue()).append("\"");
            }
        }
    }

    public String header() {
        closeHeader();
        return toString() + ">";
    }
}
