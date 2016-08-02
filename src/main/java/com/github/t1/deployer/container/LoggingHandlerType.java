package com.github.t1.deployer.container;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@JsonSerialize(using = ToStringSerializer.class)
public enum LoggingHandlerType {
    console("console"),
    periodicRotatingFile("periodic-rotating-file");

    @Getter
    private final String typeName;

    public String getHandlerName() { return typeName + "-handler"; }

    @NotNull public static LoggingHandlerType valueOfHandlerName(String typeName) {
        for (LoggingHandlerType type : LoggingHandlerType.values())
            if (type.getHandlerName().equals(typeName))
                return type;
        throw new IllegalArgumentException("No type name [" + typeName + "]");
    }

    @NotNull public static LoggingHandlerType valueOfTypeName(String typeName) {
        for (LoggingHandlerType type : LoggingHandlerType.values())
            if (type.typeName.equals(typeName))
                return type;
        throw new IllegalArgumentException("No type name [" + typeName + "]");
    }

    @Override public String toString() { return typeName; }
}
