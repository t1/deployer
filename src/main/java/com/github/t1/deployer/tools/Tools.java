package com.github.t1.deployer.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class Tools {
    public static <T> T nvl(T value, T defaultValue) { return (value == null) ? defaultValue : value; }

    public static <T> String toStringOrNull(T value) { return (value == null) ? null : value.toString(); }

    public static Map<String, String> toMap(JsonNode node) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        node.fields().forEachRemaining(field -> builder.put(field.getKey(), field.getValue().asText()));
        return builder.build();
    }
}
