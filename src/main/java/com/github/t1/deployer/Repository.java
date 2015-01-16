package com.github.t1.deployer;

import java.io.InputStream;
import java.util.List;

public interface Repository {
    public Deployment getByChecksum(CheckSum checkSum);

    public List<Deployment> availableVersionsFor(CheckSum checkSum);

    public InputStream getArtifactInputStream(CheckSum checkSum);
}
