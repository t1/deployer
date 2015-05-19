package com.github.t1.deployer.model;

import javax.xml.bind.annotation.XmlValue;

import lombok.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Value
@AllArgsConstructor
@JsonSerialize(using = ToStringSerializer.class)
public class DeploymentName {
    @NonNull
    @XmlValue
    String value;

    @SuppressWarnings("unused")
    private DeploymentName() {
        this.value = null;
    }

    @Override
    public String toString() {
        return value;
    }
}
