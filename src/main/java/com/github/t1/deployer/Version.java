package com.github.t1.deployer;

import lombok.*;


@Value
@AllArgsConstructor
@org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Version {
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
