package com.github.t1.deployer.model;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public enum LoggingHandlerType {
    console("console"),
    periodicRotatingFile("periodic-rotating-file");

    private final String typeName;

    public String getTypeName() { return typeName + "-handler"; }

    @NotNull public static LoggingHandlerType valueOfTypeName(String typeName) {
        for (LoggingHandlerType type : LoggingHandlerType.values())
            if (type.getTypeName().equals(typeName))
                return type;
        throw new IllegalArgumentException("No type name " + typeName);
    }
}
