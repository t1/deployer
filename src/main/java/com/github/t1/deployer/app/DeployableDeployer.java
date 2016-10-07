package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.DeployableAudit;
import com.github.t1.deployer.app.Audit.DeployableAudit.DeployableAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.problem.WebException.*;
import static java.util.Objects.*;

@Slf4j
public class DeployableDeployer extends AbstractDeployer<DeployableConfig, DeploymentResource, DeployableAuditBuilder> {
    private static final String WAR_SUFFIX = ".war";
    private List<DeploymentResource> existing;

    @Inject Container container;
    @Inject Repository repository;


    @Override protected void init() { this.existing = requireNonNull(container.allDeployments()); }

    @Override protected Stream<DeployableConfig> of(ConfigurationPlan plan) { return plan.deployables(); }

    @Override protected String getType() { return "deployables"; }

    @Override protected DeployableAuditBuilder buildAudit(DeploymentResource resource) {
        return DeployableAudit.builder().name(toPlanDeploymentName(resource));
    }

    @Override protected DeploymentResource getResource(DeployableConfig plan) {
        return container.deployment(getResourceDeploymentNameOf(plan)).build();
    }

    private DeploymentName getResourceDeploymentNameOf(DeployableConfig plan) {
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
    protected void update(DeploymentResource resource, DeployableConfig plan, DeployableAuditBuilder audit) {
        Artifact old = repository.lookupByChecksum(resource.checksum());
        Artifact artifact = lookupDeployedArtifact(plan, old);
        checkChecksums(plan, artifact);
        if (resource.checksum().equals(artifact.getChecksum())) {
            boolean removed = existing.removeIf(getResourceDeploymentNameOf(plan)::matches);
            assert removed : "expected [" + resource + "] to be in existing " + existing;
            log.debug("{} already deployed with same checksum {}", plan.getName(), resource.checksum());
        } else {
            container.deployment(getResourceDeploymentNameOf(plan)).build().redeploy(artifact.getInputStream());
            audit.change("checksum", resource.checksum(), artifact.getChecksum());

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

    private void checkChecksums(DeployableConfig plan, Artifact artifact) {
        if (plan.getChecksum() != null && !plan.getChecksum().equals(artifact.getChecksum()))
            throw badRequest("Repository checksum ["
                    + artifact.getChecksum()
                    + "] does not match planned checksum ["
                    + plan.getChecksum()
                    + "]");
    }

    @Override
    protected DeploymentResource buildResource(DeployableConfig plan, DeployableAuditBuilder audit) {
        Artifact artifact = lookupArtifact(plan, plan.getVersion());
        if (artifact == null)
            throw badRequest("artifact not found: " + plan);
        checkChecksums(plan, artifact);
        audit.name(plan.getName())
             .change("group-id", null, plan.getGroupId())
             .change("artifact-id", null, plan.getArtifactId())
             .change("version", null, plan.getVersion())
             .change("type", null, plan.getType())
             .change("checksum", null, artifact.getChecksum());
        return container.deployment(getResourceDeploymentNameOf(plan)).inputStream(artifact.getInputStream()).build();
    }

    private Artifact lookupDeployedArtifact(DeployableConfig plan, Artifact old) {
        Version version = plan.getVersion();
        if (version.matches("CURRENT"))
            version = old.getVersion();
        return lookupArtifact(plan, version);
    }

    private Artifact lookupArtifact(DeployableConfig plan, Version version) {
        return repository.lookupArtifact(plan.getGroupId(), plan.getArtifactId(), version, plan.getType(),
                plan.getClassifier());
    }

    @Override
    protected void auditRemove(DeploymentResource resource, DeployableConfig plan, DeployableAuditBuilder audit) {
        if (plan.getChecksum() != null && !plan.getChecksum().equals(resource.checksum()))
            throw badRequest("Planned to undeploy artifact with checksum [" + plan.getChecksum() + "] "
                    + "but deployed is [" + resource.checksum() + "]");
        Artifact old = repository.lookupByChecksum(resource.checksum());
        audit.change("group-id", old.getGroupId(), null);
        audit.change("artifact-id", old.getArtifactId(), null);
        audit.change("version", old.getVersion(), null);
        audit.change("type", old.getType(), null);
        audit.change("checksum", old.getChecksum(), null);
    }

    @Override public void cleanup(Audits audits) {
        for (DeploymentResource deployment : existing) {
            Artifact artifact = repository.lookupByChecksum(deployment.checksum());
            DeployableAuditBuilder audit = DeployableAudit.builder().name(toPlanDeploymentName(deployment));
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
            Artifact artifact = repository.lookupByChecksum(deployment.checksum());
            builder.deployable(DeployableConfig
                    .builder()
                    .name(toPlanDeploymentName(deployment))
                    .groupId(artifact.getGroupId())
                    .artifactId(artifact.getArtifactId())
                    .version(artifact.getVersion())
                    .type(artifact.getType())
                    .state(deployed)
                    .checksum(artifact.getChecksum())
                    .build());
        }
    }
}
