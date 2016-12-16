package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.DeployableAudit;
import com.github.t1.deployer.app.Audit.DeployableAudit.DeployableAuditBuilder;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.container.DeploymentResource;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Plan.PlanBuilder;
import com.github.t1.deployer.repository.Repository;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.t1.deployer.container.DeploymentResource.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.problem.WebException.*;

@Slf4j
class ArtifactDeployer extends AbstractDeployer<DeployablePlan, DeploymentResource, DeployableAuditBuilder> {
    @Inject Container container;
    @Inject Repository repository;


    @Override protected Stream<DeploymentResource> existingResources() { return container.allDeployments(); }

    @Override protected Stream<DeployablePlan> resourcesIn(Plan plan) { return plan.deployables(); }

    @Override protected String getType() { return "deployables"; }

    @Override protected DeployableAuditBuilder auditBuilder(DeploymentResource resource) {
        return DeployableAudit.builder().name(toDeploymentName(resource));
    }

    private static DeploymentName toDeploymentName(DeploymentResource resource) {
        return new DeploymentName(resource.getId());
    }

    @Override protected DeploymentResource readResource(DeployablePlan plan) {
        return container.builderFor(toDeploymentName(plan)).get();
    }

    private static DeploymentName toDeploymentName(DeployablePlan plan) {
        return (plan.getType() == war) ? new DeploymentName(plan.getName() + WAR_SUFFIX) : plan.getName();
    }

    @Override
    protected void update(DeploymentResource resource, DeployablePlan plan, DeployableAuditBuilder audit) {
        Artifact old = repository.lookupByChecksum(resource.checksum());
        Artifact artifact = lookupDeployedArtifact(plan, old);
        checkChecksums(plan, artifact);

        if (resource.checksum().equals(artifact.getChecksum())) {
            log.debug("{} already deployed with same checksum {}", plan.getName(), resource.checksum());
            return;
        }

        container.builderFor(toDeploymentName(plan)).inputStream(artifact.getInputStream()).get().redeploy();
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

    private void checkChecksums(DeployablePlan plan, Artifact artifact) {
        if (plan.getChecksum() != null && !plan.getChecksum().equals(artifact.getChecksum()))
            throw badRequest("Repository checksum ["
                    + artifact.getChecksum()
                    + "] does not match planned checksum ["
                    + plan.getChecksum()
                    + "]");
    }

    @Override
    protected Supplier<DeploymentResource> buildResource(DeployablePlan plan, DeployableAuditBuilder audit) {
        Artifact artifact = lookupArtifact(plan, plan.getVersion());
        if (artifact == null)
            throw badRequest("artifact not found: " + plan);
        checkChecksums(plan, artifact);
        audit.name(plan.getName())
             .change("group-id", null, artifact.getGroupId())
             .change("artifact-id", null, artifact.getArtifactId())
             .change("version", null, artifact.getVersion())
             .change("type", null, artifact.getType())
             .change("checksum", null, artifact.getChecksum());
        return container.builderFor(toDeploymentName(plan)).inputStream(artifact.getInputStream());
    }

    private Artifact lookupDeployedArtifact(DeployablePlan plan, Artifact old) {
        Version version = plan.getVersion();
        if (version.matches("CURRENT"))
            version = old.getVersion();
        Artifact artifact = lookupArtifact(plan, version);
        if (artifact == null)
            throw badRequest("artifact not found: " + plan + " @ " + version);
        return artifact;
    }

    private Artifact lookupArtifact(DeployablePlan plan, Version version) {
        return repository.resolveArtifact(plan.getGroupId(), plan.getArtifactId(), version, plan.getType(),
                plan.getClassifier());
    }

    @Override
    protected void auditRegularRemove(DeploymentResource resource, DeployablePlan plan, DeployableAuditBuilder audit) {
        if (plan.getChecksum() != null && !plan.getChecksum().equals(resource.checksum()))
            throw badRequest("Planned to undeploy artifact with checksum [" + plan.getChecksum() + "] "
                    + "but deployed is [" + resource.checksum() + "]");

        Artifact artifact = repository.lookupByChecksum(resource.checksum());
        auditRemove(audit, artifact);
        audit.change("checksum", artifact.getChecksum(), null);
    }

    @Override protected void cleanupRemove(DeploymentResource resource) {
        DeployableAuditBuilder audit = auditBuilder(resource);
        auditRemove(audit, repository.lookupByChecksum(resource.checksum()));
        audit.change("checksum", resource.checksum(), null);
        audits.add(audit.removed());
        resource.addRemoveStep();
    }

    private void auditRemove(DeployableAuditBuilder audit, Artifact artifact) {
        audit.change("group-id", artifact.getGroupId(), null);
        audit.change("artifact-id", artifact.getArtifactId(), null);
        audit.change("version", artifact.getVersion(), null);
        audit.change("type", artifact.getType(), null);
    }


    @Override public void read(PlanBuilder builder, DeploymentResource deployment) {
        Artifact artifact = repository.lookupByChecksum(deployment.checksum());
        builder.deployable(DeployablePlan
                .builder()
                .name(toDeploymentName(deployment))
                .groupId(artifact.getGroupId())
                .artifactId(artifact.getArtifactId())
                .version(artifact.getVersion())
                .type(artifact.getType())
                .checksum(artifact.getChecksum())
                .error(artifact.getError())
                .state(deployed)
                .build());
    }
}
