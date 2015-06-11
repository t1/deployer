package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.Deployment.*;
import static com.github.t1.deployer.tools.ErrorResponse.*;
import static com.github.t1.log.LogLevel.*;

import java.io.*;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.*;
import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.log.Logged;

@Logged(level = INFO)
@XmlRootElement(name = "deployment")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "name", "contextRoot", "checkSum", "version", "availableVersions" })
@JsonPropertyOrder({ "name", "contextRoot", "checkSum", "version", "availableVersions" })
public class DeploymentResource implements Comparable<DeploymentResource> {
    @Inject
    DeploymentContainer container;
    @Inject
    Repository repository;
    @Context
    UriInfo uriInfo;

    private Deployment deployment;
    private List<VersionInfo> availableVersions;

    @Logged(level = DEBUG)
    public DeploymentResource deployment(Deployment deployment) {
        this.deployment = deployment;
        return this;
    }

    @Logged(level = DEBUG)
    public Deployment deployment() {
        return deployment;
    }

    @JsonIgnore
    @Logged(level = DEBUG)
    public boolean isNew() {
        return getName() == null || NEW_DEPLOYMENT_NAME.equals(getName().getValue());
    }

    @GET
    public DeploymentResource self() {
        return this;
    }

    @POST
    public Response post(@Context UriInfo uriInfo, //
            @FormParam("action") String action, //
            @FormParam("contextRoot") ContextRoot contextRoot, //
            @FormParam("checksum") CheckSum checkSum //
    ) {
        switch (action) {
            case "deploy":
                if (contextRoot != null || getContextRoot() != null)
                    throw badRequest("context root to deploy must be null, not " + contextRoot + " and "
                            + getContextRoot());
                Deployment newDeployment = deploy(checkSum);
                return Response.seeOther(Deployments.path(uriInfo, newDeployment.getContextRoot())).build();
            case "redeploy":
                if (getContextRoot() != null)
                    check(contextRoot);
                redeploy(checkSum);
                return Response.seeOther(Deployments.path(uriInfo, contextRoot)).build();
            case "undeploy":
                if (getContextRoot() != null)
                    check(contextRoot);
                delete();
                return Response.seeOther(Deployments.pathAll(uriInfo)).build();
            default:
                throw badRequest("invalid action '" + action + "'");
        }
    }

    @PUT
    public Response put(@Context UriInfo uriInfo, Deployment entity) {
        check(entity.getContextRoot());
        CheckSum checkSum = entity.getCheckSum();
        if (checkSum == null)
            throw badRequest("checksum missing in " + entity);
        if (container.hasDeploymentWith(getContextRoot())) {
            redeploy(checkSum);
            return Response.noContent().build();
        } else {
            deploy(checkSum);
            return created(uriInfo);
        }
    }

    private Response created(UriInfo uriInfo) {
        return Response.created(Deployments.path(uriInfo, getContextRoot())).build();
    }

    private void check(ContextRoot contextRoot) {
        if (!Objects.equals(contextRoot, getContextRoot()))
            throw badRequest("context roots don't match: " + contextRoot + " is not " + getContextRoot());
    }

    private Deployment deploy(CheckSum checkSum) {
        Deployment newDeployment = getDeploymentFromRepository(checkSum);
        try (InputStream inputStream = repository.getArtifactInputStream(checkSum)) {
            container.deploy(newDeployment, inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newDeployment;
    }

    private void redeploy(CheckSum checkSum) {
        Deployment newDeployment = getDeploymentFromRepository(checkSum);
        try (InputStream inputStream = repository.getArtifactInputStream(checkSum)) {
            container.redeploy(newDeployment, inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Deployment getDeploymentFromRepository(CheckSum checkSum) {
        if (checkSum == null)
            throw badRequest("checksum missing");
        Deployment newDeployment = repository.getByChecksum(checkSum);
        if (newDeployment == null)
            throw notFound("no deployment with checksum " + checkSum + " found in repository");
        return newDeployment;
    }

    @DELETE
    public void delete() {
        container.undeploy(deployment);
    }

    @GET
    @Path("name")
    @XmlElement
    public DeploymentName getName() {
        return deployment.getName();
    }

    @GET
    @Path("context-root")
    @XmlElement
    public ContextRoot getContextRoot() {
        return deployment.getContextRoot();
    }

    @GET
    @Path("version")
    @XmlElement
    public Version getVersion() {
        return deployment.getVersion();
    }

    @PUT
    @Path("version")
    public Response putVersion(Version newVersion) {
        if (!container.hasDeploymentWith(getContextRoot()))
            throw badRequest("no context root: " + getContextRoot());
        for (VersionInfo available : getAvailableVersions()) {
            if (available.getVersion().equals(newVersion)) {
                redeploy(available.getCheckSum());
                return Response.noContent().build();
            }
        }
        throw notFound("no version " + newVersion + " for " + getContextRoot());
    }

    @GET
    @Path("checksum")
    @XmlElement
    public CheckSum getCheckSum() {
        return deployment.getCheckSum();
    }

    @GET
    @Path("/available-versions")
    @XmlElement(name = "availableVersion")
    @XmlElementWrapper
    public List<VersionInfo> getAvailableVersions() {
        if (availableVersions == null)
            availableVersions = repository.availableVersionsFor(deployment.getCheckSum());
        return availableVersions;
    }

    @Override
    @Logged(level = DEBUG)
    public int compareTo(DeploymentResource that) {
        return this.deployment().compareTo(that.deployment());
    }

    @Override
    @Logged(level = DEBUG)
    public String toString() {
        return "Resource:" + deployment + ((availableVersions == null) ? "" : availableVersions);
    }
}
