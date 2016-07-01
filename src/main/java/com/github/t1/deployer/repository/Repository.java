package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;

@Logged
public abstract class Repository {
    public abstract Artifact getByChecksum(Checksum checksum);

    public abstract Artifact lookupArtifact(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type);
}
