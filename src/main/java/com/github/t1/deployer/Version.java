package com.github.t1.deployer;

import lombok.*;

@Value
@AllArgsConstructor
public class Version {
    private String version;
    private boolean integration;

    /** required by JAXB, etc. */
    @SuppressWarnings("unused")
    private Version() {
        this.version = null;
        this.integration = false;
    }

    public Version(String version) {
        this.version = version;
        this.integration = version.endsWith("-SNAPSHOT");
    }

    @Override
    public String toString() {
        return version;
    }
}
