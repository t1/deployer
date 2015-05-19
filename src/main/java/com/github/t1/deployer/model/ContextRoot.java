package com.github.t1.deployer.model;

import javax.xml.bind.annotation.XmlValue;

import lombok.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Value
@AllArgsConstructor
@JsonSerialize(using = ToStringSerializer.class)
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
