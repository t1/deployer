package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.DeployableAudit;
import com.github.t1.deployer.app.Audits.Warning;
import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.container.DeploymentResource;
import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.DeployablePlan;
import com.github.t1.deployer.model.DeploymentName;
import com.github.t1.deployer.model.Plan;
import com.github.t1.deployer.model.Version;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.problemdetail.Extension;
import com.github.t1.problemdetail.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.t1.deployer.container.DeploymentResource.WAR_SUFFIX;
import static com.github.t1.deployer.model.ArtifactType.war;
import static com.github.t1.deployer.model.DeploymentState.deployed;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Slf4j
class ArtifactDeployer extends AbstractDeployer<DeployablePlan, DeploymentResource, DeployableAudit> {
    private static final String CURRENT = "CURRENT";

    @Inject Container container;
    @Inject Repository repository;

    @Override protected boolean isPinned(String name) {
        return "deployer".equals(name) || super.isPinned(name);
    }

    @Override protected Stream<DeploymentResource> existingResources() { return container.allDeployments(); }

    @Override protected Stream<DeployablePlan> resourcesIn(Plan plan) { return plan.deployables(); }

    @Override protected String getType() { return "deployables"; }

    @Override protected DeployableAudit audit(DeploymentResource resource) {
        return new DeployableAudit().setName(toDeploymentName(resource));
    }

    private static DeploymentName toDeploymentName(DeploymentResource resource) {
        return new DeploymentName(resource.getId());
    }

    @Override protected DeploymentResource readResource(DeployablePlan plan) {
        return container.builderFor(toDeploymentName(plan));
    }

    private static DeploymentName toDeploymentName(DeployablePlan plan) {
        return (plan.getType() == war) ? new DeploymentName(plan.getName() + WAR_SUFFIX) : plan.getName();
    }

    @Override
    protected void update(DeploymentResource resource, DeployablePlan plan, DeployableAudit audit) {
        Artifact old = repository.lookupByChecksum(resource.checksum());
        if (plan.getVersion().matches(CURRENT) && old.getVersion().matches("unknown")) {
            log.warn("skip update of [{}] to CURRENT: unknown checksum", plan.getName());
            return;
        }

        Artifact artifact = lookupDeployedArtifact(plan, old);
        checkChecksums(plan, artifact);

        if (resource.checksum().equals(artifact.getChecksum())) {
            log.debug("{} already deployed with same checksum {}", plan.getName(), resource.checksum());
            return;
        }

        container.builderFor(toDeploymentName(plan)).inputStream(artifact.getInputStream()).redeploy();
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
            throw new RepositoryChecksumMismatchException("Repository checksum ["
                + artifact.getChecksum()
                + "] does not match planned checksum ["
                + plan.getChecksum()
                + "]");
    }

    @Override
    protected Supplier<DeploymentResource> buildResource(DeployablePlan plan, DeployableAudit audit) {
        if (plan.getVersion().matches(CURRENT)) {
            audits.add(new Warning("skip deploying " + plan.getName() + " in version " + CURRENT));
            return () -> null;
        }
        Artifact artifact = lookupArtifact(plan, plan.getVersion());
        if (artifact == null)
            throw new ArtifactNotFoundException(plan);
        checkChecksums(plan, artifact);
        audit.setName(plan.getName())
            .change("group-id", null, artifact.getGroupId())
            .change("artifact-id", null, artifact.getArtifactId())
            .change("version", null, artifact.getVersion())
            .change("type", null, artifact.getType())
            .change("checksum", null, artifact.getChecksum());
        return () -> container.builderFor(toDeploymentName(plan)).inputStream(artifact.getInputStream());
    }

    private Artifact lookupDeployedArtifact(DeployablePlan plan, Artifact old) {
        Version version = plan.getVersion();
        if (version.matches(CURRENT))
            version = old.getVersion();
        Artifact artifact = lookupArtifact(plan, version);
        if (artifact == null)
            throw new ArtifactNotFoundException(plan, version);
        return artifact;
    }

    private Artifact lookupArtifact(DeployablePlan plan, Version version) {
        return repository.resolveArtifact(plan.getGroupId(), plan.getArtifactId(), version, plan.getType(),
            plan.getClassifier());
    }

    @Override
    protected void auditRegularRemove(DeploymentResource resource, DeployablePlan plan, DeployableAudit audit) {
        if (plan.getChecksum() != null && !plan.getChecksum().equals(resource.checksum()))
            throw new PlannedUndeployChecksumMismatchException(plan.getChecksum(), resource.checksum());

        Artifact artifact = repository.lookupByChecksum(resource.checksum());
        auditRemove(audit, artifact);
        audit.change("checksum", artifact.getChecksum(), null);
    }

    @Override protected void cleanup(DeploymentResource resource) {
        log.info("cleanup remaining {}", resource);
        DeployableAudit audit = audit(resource);
        auditRemove(audit, repository.lookupByChecksum(resource.checksum()));
        audit.change("checksum", resource.checksum(), null);
        audits.add(audit.removed());
        resource.addRemoveStep();
    }

    private void auditRemove(DeployableAudit audit, Artifact artifact) {
        audit.change("group-id", artifact.getGroupId(), null);
        audit.change("artifact-id", artifact.getArtifactId(), null);
        audit.change("version", artifact.getVersion(), null);
        audit.change("type", artifact.getType(), null);
    }


    @Override public void read(Plan plan, DeploymentResource deployment) {
        Artifact artifact = repository.lookupByChecksum(deployment.checksum());
        plan.addDeployable(
            new DeployablePlan(toDeploymentName(deployment))
                .setType(artifact.getType())
                .setError(artifact.getError())
                .setState(deployed)
                .setGroupId(artifact.getGroupId())
                .setArtifactId(artifact.getArtifactId())
                .setVersion(artifact.getVersion())
                .setChecksum(artifact.getChecksum()));
    }

    @Status(BAD_REQUEST)
    private static class RepositoryChecksumMismatchException extends RuntimeException {
        public RepositoryChecksumMismatchException(String message) { super(message); }
    }

    @Status(BAD_REQUEST)
    private static class ArtifactNotFoundException extends RuntimeException {
        public ArtifactNotFoundException(DeployablePlan plan) { super("artifact not found: " + plan); }

        public ArtifactNotFoundException(DeployablePlan plan, Version version) { super("artifact not found: " + plan + " @ " + version); }
    }

    @Status(BAD_REQUEST) @AllArgsConstructor
    public static class PlannedUndeployChecksumMismatchException extends RuntimeException {
        @Extension @Getter private Checksum planned, actual;
    }
}
