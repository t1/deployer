package com.github.t1.deployer.repository;

import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;

@Logged
public abstract class Repository {
    public abstract Deployment getByChecksum(CheckSum checkSum);

    public abstract Map<Version, CheckSum> availableVersionsFor(CheckSum checkSum);

    public abstract InputStream getArtifactInputStream(CheckSum checkSum);

    public CheckSum getChecksumForVersion(Deployment deployment, Version version) {
        for (Entry<Version, CheckSum> entry : availableVersionsFor(deployment.getCheckSum()).entrySet()) {
            if (version.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("no version " + version + " for " + deployment.getName());
    }
}
