package com.github.t1.deployer.repository;

import java.io.InputStream;
import java.util.List;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;

@Logged
public abstract class Repository {
    public abstract Deployment getByChecksum(CheckSum checkSum);

    public abstract List<VersionInfo> availableVersionsFor(CheckSum checkSum);

    public abstract InputStream getArtifactInputStream(CheckSum checkSum);

    public CheckSum getChecksumForVersion(Deployment deployment, Version version) {
        for (VersionInfo entry : availableVersionsFor(deployment.getCheckSum())) {
            if (version.equals(entry.getVersion())) {
                return entry.getCheckSum();
            }
        }
        throw new IllegalArgumentException("no version " + version + " for " + deployment.getName());
    }
}
