package com.github.t1.deployer.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LoggingHandlerType {
    console("console"),
    periodicRotatingFile("periodic-rotating-file");

    private final String typeName;

    public String getTypeName() { return typeName + "-handler"; }
}
