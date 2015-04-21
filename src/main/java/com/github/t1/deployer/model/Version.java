package com.github.t1.deployer.model;

import static javax.xml.bind.annotation.XmlAccessType.*;

import javax.xml.bind.annotation.*;

import lombok.*;

@Value
@AllArgsConstructor
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
