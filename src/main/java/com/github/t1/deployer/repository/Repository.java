package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;

import static com.github.t1.deployer.model.ArtifactType.*;

/** Stores artifacts, e.g. Maven Central or Artifactory */
@Slf4j
@Logged
public abstract class Repository {
    /** find artifact in repository or return a dummy representing `unknown` or `error`. */
    public Artifact lookupByChecksum(Checksum checksum) {
        if (checksum == null || checksum.isEmpty())
            return errorArtifact(checksum, "empty checksum");
        try {
            Artifact artifact = searchByChecksum(checksum);
            if (!artifact.getChecksumRaw().equals(checksum))
                throw new AssertionError("expected checksum from repository [" + artifact.getChecksumRaw() + "] "
                        + "to be equal to the checksum requested with [" + checksum + "]");
            return artifact;
        } catch (UnknownChecksumException e) {
            return errorArtifact(checksum, "unknown");
        } catch (RuntimeException e) {
            log.error("error retrieving artifact by checksum " + checksum, e);
            return errorArtifact(checksum, "error");
        }
    }

    private Artifact errorArtifact(Checksum checksum, String messageArtifactId) {
        return Artifact
                .builder()
                .groupId(new GroupId("*error*"))
                .artifactId(new ArtifactId(messageArtifactId))
                .version(new Version("unknown"))
                .type(unknown)
                .checksum(checksum)
                .inputStreamSupplier(() -> {
                    throw new UnsupportedOperationException();
                })
                .build();
    }

    public abstract Artifact searchByChecksum(Checksum checksum);

    public abstract Artifact lookupArtifact(GroupId groupId, ArtifactId artifactId, Version version,
            Classifier classifier, ArtifactType type);
}
