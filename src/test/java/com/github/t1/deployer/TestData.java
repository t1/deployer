package com.github.t1.deployer;

import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;

public class TestData {
    public static Deployment deploymentFor(ContextRoot contextRoot, Version version) {
        return new Deployment(nameFor(contextRoot), contextRoot, fakeChecksumFor(contextRoot, version), version);
    }

    public static Deployment deploymentFrom(InputStream inputStream) {
        String string = inputStream.toString();
        assert string.startsWith("[") && string.endsWith("]");
        string = string.substring(1, string.length() - 1);
        String[] parts = string.split("@");
        assert parts.length == 2;
        ContextRoot contextRoot = new ContextRoot(parts[0]);
        Version version = new Version(parts[1]);
        DeploymentName name = nameFor(contextRoot);
        CheckSum checkSum = fakeChecksumFor(contextRoot, version);
        return new Deployment(name, contextRoot, checkSum, version);
    }

    public static DeploymentName nameFor(ContextRoot contextRoot) {
        return new DeploymentName(contextRoot + ".war");
    }

    public static Deployment deploymentFor(ContextRoot contextRoot) {
        return deploymentFor(contextRoot, fakeVersionFor(contextRoot));
    }

    public static String failedCli(String message) {
        return "{\"outcome\" => \"failed\",\n" //
                + "\"failure-description\" => \"" + message + "\n" //
                + "}\n";
    }

    public static String successCli(String result) {
        return "{\n" //
                + "\"outcome\" => \"success\",\n" //
                + "\"result\" => " + result + "\n" //
                + "}\n";
    }

    public static void givenDeployments(Repository repository, ContextRoot... contextRoots) {
        for (ContextRoot contextRoot : contextRoots) {
            List<Release> releases = releases(contextRoot);
            for (Release release : releases) {
                Version version = release.getVersion();
                CheckSum checksum = fakeChecksumFor(contextRoot, version);
                when(repository.getByChecksum(checksum)).thenReturn(deploymentFor(contextRoot, version));
                when(repository.releasesFor(checksum)).thenReturn(releases);
                when(repository.getArtifactInputStream(checksum)) //
                        .thenReturn(inputStreamFor(contextRoot, version));
            }
        }
    }

    public static List<Release> releases(ContextRoot contextRoot) {
        List<Version> versions = fakeVersionsFor(contextRoot);
        return releases(contextRoot, versions);
    }

    private static List<Release> releases(ContextRoot contextRoot, List<Version> versions) {
        List<Release> result = new ArrayList<>();
        for (Version version : versions) {
            result.add(new Release(version, fakeChecksumFor(contextRoot, version)));
        }
        return result;
    }

    public static void givenDeployments(final DeploymentContainer container, ContextRoot... contextRoots) {
        final List<Deployment> deployments = new ArrayList<>();
        for (ContextRoot contextRoot : contextRoots) {
            Deployment deployment = deploymentFor(contextRoot);
            givenDeployment(container, deployments, deployment);
        }
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                DeploymentName deploymentName = invocation.getArgumentAt(0, DeploymentName.class);
                Deployment deployment = deploymentFrom(invocation.getArgumentAt(1, InputStream.class));
                assert deploymentName.equals(deployment.getName());
                givenDeployment(container, deployments, deployment);
                return null;
            }
        }).when(container).deploy(any(DeploymentName.class), any(InputStream.class));
        when(container.getAllDeployments()).thenReturn(deployments);
    }

    public static void givenDeployment(DeploymentContainer container, List<Deployment> deployments,
            Deployment deployment) {
        ContextRoot contextRoot = deployment.getContextRoot();
        when(container.getDeploymentFor(contextRoot)).thenReturn(deployment);
        when(container.hasDeploymentWith(contextRoot)).thenReturn(true);
        when(container.getDeploymentWith(deployment.getCheckSum())).thenReturn(deployment);
        deployments.add(deployment);
    }

    public static String deploymentJson(ContextRoot contextRoot) {
        return deploymentJson(contextRoot, fakeVersionFor(contextRoot));
    }

    public static String deploymentJson(ContextRoot contextRoot, Version version) {
        return "{" //
                + "\"name\":\"" + nameFor(contextRoot) + "\"," //
                + "\"contextRoot\":\"" + contextRoot + "\"," //
                + "\"checkSum\":\"" + fakeChecksumFor(contextRoot, version) + "\"," //
                + "\"version\":\"" + version + "\"" //
                + "}";
    }

    public static void assertStatus(Status status, Response response) {
        if (status.getStatusCode() != response.getStatus()) {
            StringBuilder message = new StringBuilder();
            message.append("expected ").append(status.getStatusCode()).append(" ");
            message.append(status.getReasonPhrase().toUpperCase());
            message.append(" but got ").append(response.getStatus()).append(" ");
            message.append(Status.fromStatusCode(response.getStatus()).getReasonPhrase().toUpperCase());

            String entity = response.readEntity(String.class);
            if (entity != null)
                message.append(":\n" + entity);

            fail(message.toString());
        }
    }

    public static void assertDeployment(ContextRoot contextRoot, Deployment deployment) {
        assertDeployment(contextRoot, fakeVersionFor(contextRoot), deployment);
    }

    public static void assertDeployment(ContextRoot contextRoot, Version expectedVersion, Deployment deployment) {
        assertEquals(contextRoot, deployment.getContextRoot());
        assertEquals(nameFor(contextRoot), deployment.getName());
        assertEquals(expectedVersion, deployment.getVersion());
    }
}
