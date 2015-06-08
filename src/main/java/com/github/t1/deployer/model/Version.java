package com.github.t1.deployer.model;

import static javax.xml.bind.annotation.XmlAccessType.*;

import javax.xml.bind.annotation.*;

import lombok.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Value
@AllArgsConstructor
@XmlAccessorType(NONE)
@JsonSerialize(using = ToStringSerializer.class)
public class Version {
    public static final Version UNKNOWN = new Version("unknown");

    @NonNull
    @XmlValue
    private String version;

    /** required for JAXB, etc. */
    @SuppressWarnings("unused")
    private Version() {
        this.version = null;
    }

    @Override
    public String toString() {
        return version;
    }
}
