package com.github.t1.deployer.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@JsonSerialize(using = ToStringSerializer.class)
public enum LogHandlerType {
    console("console"),
    periodicRotatingFile("periodic-rotating-file"),
    custom("custom");

    @Getter
    private final String typeName;

    public String getHandlerTypeName() { return typeName + "-handler"; }

    @NotNull public static LogHandlerType valueOfHandlerName(String typeName) {
        for (LogHandlerType type : LogHandlerType.values())
            if (type.getHandlerTypeName().equals(typeName))
                return type;
        throw new IllegalArgumentException("No type name [" + typeName + "]");
    }

    @NotNull public static LogHandlerType valueOfTypeName(String typeName) {
        for (LogHandlerType type : LogHandlerType.values())
            if (type.typeName.equals(typeName))
                return type;
        throw new IllegalArgumentException("No type name [" + typeName + "]");
    }

    @Override public String toString() { return typeName; }
}
