package com.github.t1.deployer;

import javax.xml.bind.annotation.XmlValue;

import lombok.*;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ser.std.ToStringSerializer;

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