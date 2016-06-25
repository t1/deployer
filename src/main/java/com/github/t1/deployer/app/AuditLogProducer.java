package com.github.t1.deployer.app;

import com.github.t1.deployer.container.Audit.ArtifactAudit;
import com.github.t1.deployer.container.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.container.AuditLog;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.repository.*;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class AuditLogProducer {
    @Inject Repository repository;

    @Produces public AuditLog produceAuditLog() {
        return new AuditLog(this::artifactLookup);
    }

    private ArtifactAuditBuilder artifactLookup(Checksum checksum) {
        Artifact artifact = repository.getByChecksum(checksum);
        return ArtifactAudit.of(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }
}
