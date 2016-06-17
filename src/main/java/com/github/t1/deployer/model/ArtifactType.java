package com.github.t1.deployer.model;

public enum ArtifactType {
    war, jar;

    public String extension() { return name(); }

    public String type() { return name(); }
}
