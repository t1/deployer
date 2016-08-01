package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.ArtifactAudit;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static java.util.Objects.*;

@Slf4j
public class ArtifactDeployer extends AbstractDeployer<DeploymentConfig, DeploymentResource, ArtifactAuditBuilder> {
    private static final String WAR_SUFFIX = ".war";
    private final boolean managed;
    private final Repository repository;
    private final Container container;
    private final List<DeploymentResource> existing;
    private final Function<Checksum, Artifact> lookupByChecksum;

    public ArtifactDeployer(Repository repository, Container container, boolean managed, Audits audits,
            Function<Checksum, Artifact> lookupByChecksum) {
        super(audits);
        this.repository = requireNonNull(repository);
        this.container = requireNonNull(container);
        this.managed = managed;
        this.lookupByChecksum = requireNonNull(lookupByChecksum);

        this.existing = requireNonNull(container.allDeployments());
    }

    @Override protected ArtifactAuditBuilder buildAudit(DeploymentResource resource) {
        return ArtifactAudit.builder().name(toPlanDeploymentName(resource));
    }

    @Override protected DeploymentResource getResource(DeploymentConfig plan) {
        return container.deployment(toResourceDeploymentName(plan)).build();
    }

    private DeploymentName toResourceDeploymentName(DeploymentConfig plan) {
        if (plan.getType() == war && !plan.getName().getValue().endsWith(".war"))
            return new DeploymentName(plan.getName() + ".war");
        return plan.getName();
    }

    private DeploymentName toPlanDeploymentName(DeploymentResource resource) {
        String name = resource.name().getValue();
        if (name.endsWith(WAR_SUFFIX))
            return new DeploymentName(name.substring(0, name.length() - WAR_SUFFIX.length()));
        return resource.name();
    }

    @Override
    protected void update(DeploymentResource resource, DeploymentConfig plan, ArtifactAuditBuilder audit) {
        Artifact artifact = lookupArtifact(plan);
        if (resource.checksum().equals(artifact.getChecksum())) {
            boolean removed = existing.removeIf(toResourceDeploymentName(plan)::matches);
            assert removed : "expected [" + resource + "] to be in existing " + existing;
            log.info("already deployed with same checksum: {}", plan.getName());
        } else {
            container.deployment(toResourceDeploymentName(plan)).build().redeploy(artifact.getInputStream());
            audit.change("checksum", resource.checksum(), artifact.getChecksum());

            Artifact old = lookupByChecksum.apply(resource.checksum());
            if (!Objects.equals(old.getGroupId(), artifact.getGroupId()))
                audit.change("group-id", old.getGroupId(), artifact.getGroupId());
            if (!Objects.equals(old.getArtifactId(), artifact.getArtifactId()))
                audit.change("artifact-id", old.getArtifactId(), artifact.getArtifactId());
            if (!Objects.equals(old.getVersion(), artifact.getVersion()))
                audit.change("version", old.getVersion(), artifact.getVersion());
            if (!Objects.equals(old.getType(), artifact.getType()))
                audit.change("type", old.getType(), artifact.getType());
        }
    }

    @Override
    protected DeploymentResource buildResource(DeploymentConfig plan, ArtifactAuditBuilder audit) {
        Artifact artifact = lookupArtifact(plan);
        assert artifact != null : "not found: " + plan;
        audit.name(plan.getName())
             .change("group-id", null, plan.getGroupId())
             .change("artifact-id", null, plan.getArtifactId())
             .change("version", null, plan.getVersion())
             .change("type", null, plan.getType())
             .change("checksum", null, artifact.getChecksum());
        return container.deployment(toResourceDeploymentName(plan)).inputStream(artifact.getInputStream()).build();
    }

    private Artifact lookupArtifact(DeploymentConfig plan) {
        return repository.lookupArtifact(plan.getGroupId(), plan.getArtifactId(), plan.getVersion(), plan.getType());
    }

    @Override
    protected void auditRemove(DeploymentResource resource, DeploymentConfig plan, ArtifactAuditBuilder audit) {
        audit.change("group-id", plan.getGroupId(), null);
        audit.change("artifact-id", plan.getArtifactId(), null);
        audit.change("version", plan.getVersion(), null);
        audit.change("type", plan.getType(), null);
        audit.change("checksum", resource.checksum(), null);
    }

    @Override public void cleanup(Audits audits) {
        if (managed)
            for (DeploymentResource deployment : existing) {
                Artifact artifact = lookupByChecksum.apply(deployment.checksum());
                ArtifactAuditBuilder audit = ArtifactAudit.builder().name(toPlanDeploymentName(deployment));
                audit.change("group-id", artifact.getGroupId(), null);
                audit.change("artifact-id", artifact.getArtifactId(), null);
                audit.change("version", artifact.getVersion(), null);
                audit.change("type", artifact.getType(), null);
                audit.change("checksum", deployment.checksum(), null);
                audits.audit(audit.removed());
                deployment.remove();
            }
    }


    @Override public void read(ConfigurationPlanBuilder builder) {
        for (DeploymentResource deployment : container.allDeployments()) {
            Artifact artifact = lookupByChecksum.apply(deployment.checksum());
            builder.artifact(DeploymentConfig
                    .builder()
                    .name(toPlanDeploymentName(deployment))
                    .groupId(artifact.getGroupId())
                    .artifactId(artifact.getArtifactId())
                    .version(artifact.getVersion())
                    .type(artifact.getType())
                    .state(deployed)
                    .build());
        }
    }
}
