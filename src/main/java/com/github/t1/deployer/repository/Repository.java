package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;

import java.io.InputStream;
import java.util.List;

@Logged
public abstract class Repository {
    public static final Version ERROR = new Version("error");
    public static final Version NO_CHECKSUM = new Version("no checksum");
    public static final Version UNKNOWN = new Version("unknown");

    public abstract Deployment getByChecksum(CheckSum checkSum);

    public abstract List<Release> releasesFor(CheckSum checkSum);

    public abstract InputStream getArtifactInputStream(CheckSum checkSum);

    public CheckSum getChecksumForVersion(Deployment deployment, Version version) {
        for (Release release : releasesFor(deployment.getCheckSum()))
            if (version.equals(release.getVersion()))
                return release.getCheckSum();
        throw new IllegalArgumentException("no version " + version + " for " + deployment.getName());
    }

    public abstract Artifact buildArtifact(GroupId groupId, ArtifactId artifactId, Version version);
}
