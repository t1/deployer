package com.github.t1.deployer.model;

import org.joda.convert.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Value;

@Value
@JsonSerialize(using = ToStringSerializer.class)
public class Password {
    String value;

    @FromString
    public Password(String value) {
        this.value = value;
    }

    @Override
    @ToString
    public String toString() {
        return "undisclosed password";
    }
}
