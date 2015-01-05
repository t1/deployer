package com.github.t1.deployer;

import java.io.InputStream;
import java.util.List;


public interface Repository {

    public List<Version> availableVersionsFor(String md5sum);

    public Version searchByChecksum(String md5sum);

    public InputStream getArtifactInputStream(String md5sum);

}