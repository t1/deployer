package com.github.t1.deployer;

import java.util.List;

import lombok.*;

@ToString
@RequiredArgsConstructor
public class Deployment {
    private final VersionsGateway versionsGateway;

    private final String contextRoot;
    private final String hash;

    private Version version;
    private List<Version> availableVersions;

    /** required by JAXB, etc. */
    @Deprecated
    public Deployment() {
        this.versionsGateway = null;
        this.contextRoot = null;
        this.hash = null;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public Version getVersion() {
        if (version == null)
            version = versionsGateway.searchByChecksum(hash);
        return version;
    }

    public List<Version> getAvailableVersions() {
        if (availableVersions == null)
            availableVersions = versionsGateway.searchVersions(groupId(), artifactId());
        return availableVersions;
    }

    // FIXME real group ids
    public String groupId() {
        return strippedContextRoot() + "-group";
    }

    // FIXME real artifact ids
    public String artifactId() {
        return strippedContextRoot() + "-artifact";
    }

    private String strippedContextRoot() {
        assert contextRoot.startsWith("/");
        return contextRoot.substring(1);
    }
}
