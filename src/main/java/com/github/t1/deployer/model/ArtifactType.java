package com.github.t1.deployer.model;

public enum ArtifactType {
    /** deployables */
    war, jar, ear,
    /** non-deployables: */
    pom, bundle, unknown;

    public String extension() { return name(); }

    public String type() { return name(); }
}
