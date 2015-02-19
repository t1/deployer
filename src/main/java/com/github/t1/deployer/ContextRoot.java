package com.github.t1.deployer;

import javax.xml.bind.annotation.XmlValue;

import lombok.*;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ser.std.ToStringSerializer;

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
