package com.github.t1.deployer.model;

import javax.xml.bind.annotation.XmlValue;

import lombok.*;

@Value
@AllArgsConstructor
@com.fasterxml.jackson.databind.annotation.JsonSerialize(
        using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@org.codehaus.jackson.map.annotate.JsonSerialize(using = org.codehaus.jackson.map.ser.std.ToStringSerializer.class)
public class ContextRoot {
    @NonNull
    @XmlValue
    String value;

    @SuppressWarnings("unused")
    private ContextRoot() {
        this.value = null;
    }

    @Override
    public String toString() {
        return value;
    }
}
