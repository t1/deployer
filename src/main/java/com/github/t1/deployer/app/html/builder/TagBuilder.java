package com.github.t1.deployer.app.html.builder;

import java.util.*;

public class TagBuilder extends BaseBuilder {
    private final String tagName;
    private Map<String, String> attributes;
    private boolean headerClosed = false;

    public TagBuilder(String tagName) {
        super(); // explicitly no container
        this.tagName = tagName;
    }

    public TagBuilder attribute(String name, Object value) {
        if (attributes == null)
            attributes = new LinkedHashMap<>();
        attributes.put(name, value == null ? null : value.toString());
        return this;
    }

    public TagBuilder classes(String... strings) {
        StringBuilder out = new StringBuilder();
        for (String string : strings) {
            if (out.length() > 0)
                out.append(" ");
            out.append(string);
        }
        attribute("class", out.toString());
        return this;
    }

    public TagBuilder name(String name) {
        return attribute("name", name);
    }

    public TagBuilder id(String id) {
        return attribute("id", id);
    }

    @Override
    public BaseBuilder close() {
        closeHeader();
        rawAppend("/>");
        return this;
    }

    public Object body(Object body) {
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
}
