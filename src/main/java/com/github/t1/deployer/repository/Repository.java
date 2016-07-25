package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;

/** Stores artifacts, e.g. Maven Central or Artifactory */
@Logged
public abstract class Repository {
    public abstract Artifact searchByChecksum(ChecksumX checksum);

    public abstract Artifact lookupArtifact(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type);
}
