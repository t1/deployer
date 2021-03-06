package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.ArtifactType;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.Classifier;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.Version;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BadRequestException;
import java.util.Comparator;
import java.util.List;

import static com.github.t1.deployer.model.ArtifactType.unknown;

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
            return errorArtifact(checksum, "error retrieving artifact");
        }
    }

    private Artifact errorArtifact(Checksum checksum, String error) {
        return new Artifact()
            .setGroupId(new GroupId("unknown"))
            .setArtifactId(new ArtifactId("unknown"))
            .setVersion(new Version("unknown"))
            .setType(unknown)
            .setChecksum(checksum)
            .setError(error)
            .setInputStreamSupplier(() -> {
                throw new UnsupportedOperationException();
            });
    }

    public abstract Artifact searchByChecksum(Checksum checksum);

    public final Artifact resolveArtifact(GroupId groupId, ArtifactId artifactId, Version version,
                                          ArtifactType type, Classifier classifier) {
        if ("LATEST".equals(version.getValue()))
            version = findVersion(groupId, artifactId, false, version);
        else if ("UNSTABLE".equals(version.getValue()))
            version = findVersion(groupId, artifactId, true, version);
        return lookupArtifact(groupId, artifactId, version, type, classifier);
    }

    private Version findVersion(GroupId groupId, ArtifactId artifactId, boolean snapshots, Version versionExpression) {
        List<Version> versions = listVersions(groupId, artifactId, false);
        if (snapshots)
            versions.addAll(listVersions(groupId, artifactId, true));
        Version max = versions.stream().max(Comparator.naturalOrder())
            .orElseThrow(() -> new BadRequestException("no versions found for " + groupId + ":" + artifactId));
        log.debug("resolved {}:{} {} to {}", groupId, artifactId, versionExpression, max);
        return max;
    }

    protected abstract Artifact lookupArtifact(GroupId groupId, ArtifactId artifactId, Version version,
                                               ArtifactType type, Classifier classifier);

    public abstract List<Version> listVersions(GroupId groupId, ArtifactId artifactId, boolean snapshot);
}
