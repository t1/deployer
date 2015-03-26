package com.github.t1.deployer.repository;

import java.io.InputStream;
import java.util.List;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;

@Logged
public abstract class Repository {
    public abstract Deployment getByChecksum(CheckSum checkSum);

    public abstract List<Deployment> availableVersionsFor(CheckSum checkSum);

    public abstract InputStream getArtifactInputStream(CheckSum checkSum);

    public Deployment getChecksumForVersion(Deployment deployment, Version version) {
        for (Deployment other : availableVersionsFor(deployment.getCheckSum())) {
            if (version.equals(other.getVersion())) {
                return other;
            }
        }
        throw new IllegalArgumentException("no version " + version + " for " + deployment.getName());
    }
}
