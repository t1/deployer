package com.github.t1.deployer;

import java.io.InputStream;
import java.util.List;

public interface Repository {
    public Version getVersionByChecksum(String md5sum);

    public List<Version> availableVersionsFor(String md5sum);

    public InputStream getArtifactInputStream(String md5sum);
}
