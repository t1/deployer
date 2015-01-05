package com.github.t1.deployer;

import java.io.InputStream;
import java.util.List;


public interface Repository {

    public List<Version> availableVersionsFor(Deployment deployment);

    public Version searchByChecksum(String md5sum);

    public InputStream getArtifactInputStream(Deployment deployment);

}