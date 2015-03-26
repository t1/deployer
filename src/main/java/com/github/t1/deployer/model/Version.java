package com.github.t1.deployer.model;

import static javax.xml.bind.annotation.XmlAccessType.*;

import javax.xml.bind.annotation.*;

import lombok.*;

@Value
@AllArgsConstructor
@org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.codehaus.jackson.map.annotate.JsonSerialize(using = org.codehaus.jackson.map.ser.std.ToStringSerializer.class,
        include = org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL)
@com.fasterxml.jackson.databind.annotation.JsonSerialize(
        using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@XmlAccessorType(NONE)
public class Version {
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
