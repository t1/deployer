package com.github.t1.deployer;

import java.io.InputStream;
import java.util.List;

public interface Repository {
    public Version getVersionByChecksum(CheckSum checkSum);

    public List<Version> availableVersionsFor(CheckSum checkSum);

    public InputStream getArtifactInputStream(CheckSum checkSum);
}
