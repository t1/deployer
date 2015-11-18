package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.Deployment.*;
import static javax.ws.rs.core.Response.Status.*;

import java.io.*;
import java.net.URI;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.container.DeploymentContainer.DeploymentOperationFailed;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.ramlap.ApiResponse;
import com.github.t1.ramlap.ProblemDetail.*;

import io.swagger.annotations.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Api(tags = "deployments")
@Boundary
@Path("/deployments")
@Slf4j
public class Deployments {
    public static class NoCheckSum extends ValidationFailed {}

    public static class NoContextRoot extends ValidationFailed {}

    public static class DeploymentNotFound extends NotFound {}

    @ApiResponse(title = "no release with version found")
    public static class ReleaseNotFound extends NotFound {}

    private static final Comparator<Release> DESCENDING_RELEASE_ORDER = Release.BY_VERSION.reversed();

    public enum PostDeploymentAction {
        redeploy,
        undeploy;
    }

    private static UriBuilder baseBuilder(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(Deployments.class);
    }

    public static URI base(UriInfo uriInfo) {
        return baseBuilder(uriInfo).build();
    }

    public static URI pathAll(UriInfo uriInfo) {
        return base(uriInfo);
    }

    public static URI path(UriInfo uriInfo, ContextRoot contextRoot) {
        return baseBuilder(uriInfo).path(contextRoot.getValue()).build();
    }

    public static URI newDeployment(UriInfo uriInfo) {
        return baseBuilder(uriInfo).path(NEW_DEPLOYMENT_PATH).build();
    }

    @Inject
    DeploymentContainer container;
    @Inject
    Repository repository;
    @Context
    UriInfo uriInfo;

    @GET
    @ApiOperation("list all deployments")
    public List<Deployment> getAllDeployments() {
        List<Deployment> deployments = new ArrayList<>();
        for (Deployment deployment : container.getAllDeployments()) {
            deployments.add(withVersion(deployment));
        }
        return deployments;
    }

    private Deployment withVersion(Deployment deployment) {
        Deployment byChecksum = repository.getByChecksum(deployment.getCheckSum());
        return deployment.withVersion(byChecksum.getVersion());
    }

    @GET
    @Path(NEW_DEPLOYMENT_PATH)
    @ApiOperation(hidden = true, value = "return a form for new deployments")
    public Deployment newDeployment() {
        return NEW_DEPLOYMENT;
    }

    @GET
    @Path("/{contextRoot}")
    @ApiOperation("get a deployment")
    public Deployment getByContextRoot(@PathParam("contextRoot") ContextRoot contextRoot) {
        Deployment deployment = container.getDeploymentFor(contextRoot);
        if (deployment == null)
            deployment = new Deployment(contextRoot);
        List<Release> releases = repository.releasesFor(deployment.getCheckSum());
        releases.sort(DESCENDING_RELEASE_ORDER);
        return withVersion(deployment).withReleases(releases);
    }

    @POST
    @ApiOperation("post a new deployment")
    @ApiResponse(type = DeploymentOperationFailed.class)
    public Response post( //
            @Context UriInfo uriInfo, //
            @FormParam("checksum") @ApiParam(required = true) CheckSum checkSum, //
            @FormParam("name") @ApiParam("optional name; defaults to the name in the repository") DeploymentName name //
    ) {
        Deployment newDeployment = deploy(checkSum, name);
        ContextRoot contextRoot = newDeployment.getContextRoot();
        return Response.seeOther(Deployments.path(uriInfo, contextRoot)).build();
    }

    @POST
    @Path("/{contextRoot}")
    @ApiOperation("post an action on an existing deployment")
    @ApiResponses({ //
            @io.swagger.annotations.ApiResponse(code = 400, message = "action or checksum form parameter is missing"), //
            @io.swagger.annotations.ApiResponse(code = 303, message = "redirect to redeployed deployment") //
    })
    public Response postToContextRoot( //
            @Context UriInfo uriInfo, //
            @PathParam("contextRoot") ContextRoot contextRoot, //
            @FormParam("action") @ApiParam(required = true) PostDeploymentAction action, //
            @FormParam("checksum") @ApiParam(required = true) CheckSum checkSum //
    ) {
        if (action == null)
            return Response.status(BAD_REQUEST).entity("action form parameter is missing").build();
        switch (action) {
        case redeploy:
            check(contextRoot, getDeploymentFromRepository(checkSum).getContextRoot());
            redeploy(checkSum);
            return Response.seeOther(Deployments.path(uriInfo, contextRoot)).build();
        case undeploy:
            if (checkSum == null)
                return Response.status(BAD_REQUEST).entity("checksum form parameter is missing").build();
            Deployment newDeployment = getDeploymentFromRepository(checkSum);
            if (newDeployment == null)
                log.warn("undeploying deployment with checksum " + checkSum + " not found in repository");
            else if (Repository.UNKNOWN.equals(newDeployment.getVersion()))
                log.warn("undeploying deployment with unknown version: " + contextRoot);
            else
                check(contextRoot, newDeployment.getContextRoot());
            delete(contextRoot);
            return Response.seeOther(Deployments.pathAll(uriInfo)).build();
        }
        throw new RuntimeException("unreachable code");
    }

    @PUT
    @Path("/{contextRoot}")
    @ApiOperation("create or update a deployment")
    public Response put( //
            @Context UriInfo uriInfo, //
            @PathParam("contextRoot") ContextRoot contextRoot, //
            Deployment entity //
    ) {
        check(contextRoot, entity.getContextRoot());
        CheckSum checkSum = entity.getCheckSum();
        if (checkSum == null)
            return new NoCheckSum().toResponse();
        if (container.hasDeploymentWith(contextRoot)) {
            redeploy(checkSum);
            return Response.noContent().build();
        } else {
            deploy(checkSum, entity.getName());
            return created(uriInfo, contextRoot);
        }
    }

    private Response created(UriInfo uriInfo, ContextRoot contextRoot) {
        return Response.created(Deployments.path(uriInfo, contextRoot)).build();
    }

    private void check(ContextRoot left, ContextRoot right) {
        if (!Objects.equals(left, right))
            throw new BadRequest().detail("context roots don't match: " + left + " is not " + right).toWebException();
    }

    @SneakyThrows(IOException.class)
    private Deployment deploy(CheckSum checkSum, DeploymentName nameOverride) {
        Deployment newDeployment = getDeploymentFromRepository(checkSum);
        if (hasNameOverride(nameOverride)) {
            log.info("overwrite deployment name {} with {}", newDeployment.getName(), nameOverride);
            newDeployment = newDeployment.withName(nameOverride);
        }
        try (InputStream inputStream = repository.getArtifactInputStream(checkSum)) {
            container.deploy(newDeployment.getName(), inputStream);
        }
        return newDeployment;
    }

    private boolean hasNameOverride(DeploymentName name) {
        return name != null && name.getValue() != null && !name.getValue().isEmpty();
    }

    @SneakyThrows(IOException.class)
    private void redeploy(CheckSum checkSum) {
        Deployment newDeployment = getDeploymentFromRepository(checkSum);
        try (InputStream inputStream = repository.getArtifactInputStream(checkSum)) {
            container.redeploy(newDeployment.getName(), inputStream);
        }
    }

    private Deployment getDeploymentFromRepository(CheckSum checkSum) {
        if (checkSum == null)
            throw new NoCheckSum().toWebException();
        Deployment newDeployment = repository.getByChecksum(checkSum);
        if (newDeployment == null)
            throw new DeploymentNotFound().detail("no deployment with checksum " + checkSum + " found in repository")
                    .toWebException();
        return newDeployment;
    }

    @DELETE
    @Path("/{contextRoot}")
    @ApiOperation("delete a deployment")
    public void delete(@PathParam("contextRoot") ContextRoot contextRoot) {
        Deployment deployment = container.getDeploymentFor(contextRoot);
        container.undeploy(deployment.getName());
    }

    @GET
    @Path("/{contextRoot}/name")
    @ApiOperation("get the name of a deployment")
    public DeploymentName getName(@PathParam("contextRoot") ContextRoot contextRoot) {
        return container.getDeploymentFor(contextRoot).getName();
    }

    @GET
    @Path("/{contextRoot}/version")
    @ApiOperation("get the version of a deployment")
    public Version getVersion(@PathParam("contextRoot") ContextRoot contextRoot) {
        return withVersion(container.getDeploymentFor(contextRoot)).getVersion();
    }

    @PUT
    @Path("/{contextRoot}/version")
    @ApiOperation("put the version of a deployment, triggering a redeploy")
    @ApiResponse(status = NO_CONTENT, title = "Okay. New version is deployed")
    @ApiResponse(type = NoContextRoot.class)
    @ApiResponse(type = ReleaseNotFound.class)
    @ApiResponse(type = InternalServerError.class, title = "deployment didn't work")
    public Response putVersion(@PathParam("contextRoot") ContextRoot contextRoot, Version newVersion) {
        if (!container.hasDeploymentWith(contextRoot))
            throw new NoContextRoot().detail("no context root: " + contextRoot).toWebException();
        for (Release release : getReleases(contextRoot)) {
            if (release.getVersion().equals(newVersion)) {
                redeploy(release.getCheckSum());
                return Response.noContent().build();
            }
        }
        return new ReleaseNotFound().detail("no released version " + newVersion + " for " + contextRoot).toResponse();
    }

    @GET
    @Path("/{contextRoot}/checksum")
    @ApiOperation("get the checksum of a deployment")
    public CheckSum getCheckSum(@PathParam("contextRoot") ContextRoot contextRoot) {
        return container.getDeploymentFor(contextRoot).getCheckSum();
    }

    @GET
    @Path("/{contextRoot}/releases")
    @ApiOperation("get the releases of a deployment")
    public List<Release> getReleases(@PathParam("contextRoot") ContextRoot contextRoot) {
        Deployment deployment = container.getDeploymentFor(contextRoot);
        List<Release> releases = repository.releasesFor(deployment.getCheckSum());
        releases.sort(DESCENDING_RELEASE_ORDER);
        return releases;
    }
}
