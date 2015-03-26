package com.github.t1.deployer;

import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;

import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import lombok.SneakyThrows;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;

public class TestData {
    public static Deployment deploymentFor(ContextRoot contextRoot, Version version) {
        Deployment deployment =
                new Deployment(nameFor(contextRoot), contextRoot, fakeChecksumFor(contextRoot, version));
        deployment.setVersion(version);
        return deployment;
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
        return new Deployment(name, contextRoot, checkSum);
    }

    public static DeploymentName nameFor(ContextRoot contextRoot) {
        return new DeploymentName(contextRoot + ".war");
    }

    public static Deployment deploymentFor(ContextRoot contextRoot) {
        return deploymentFor(contextRoot, fakeVersionFor(contextRoot));
    }

    public static ModelNode readAllDeploymentsCli() {
        ModelNode node = new ModelNode();
        node.get("address").add("deployment", "*");
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private static String readDeploymentsCliResult(ContextRoot... contextRoots) {
        StringBuilder out = new StringBuilder();
        for (ContextRoot contextRoot : contextRoots) {
            if (out.length() == 0)
                out.append("[");
            else
                out.append(",");
            out.append("{\n" //
                    + "\"address\" => [(\"deployment\" => \"" + nameFor(contextRoot) + "\")],\n" //
                    + "\"outcome\" => \"success\",\n" //
                    + "\"result\" => " + deploymentCli(contextRoot) + "\n" //
                    + "}\n");
        }
        out.append("]");
        return out.toString();
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

    public static String[] deploymentsCli(ContextRoot... contextRoots) {
        String[] result = new String[contextRoots.length];
        for (int i = 0; i < contextRoots.length; i++) {
            result[i] = deploymentCli(contextRoots[i]);
        }
        return result;
    }

    public static String deploymentCli(ContextRoot contextRoot) {
        return "{\n" //
                + "\"content\" => [{\"hash\" => bytes {\n" //
                + fakeChecksumFor(contextRoot).hexByteArray() //
                + "}}],\n" //
                + "\"enabled\" => true,\n" //
                + ("\"name\" => \"" + nameFor(contextRoot) + "\",\n") //
                + "\"persistent\" => true,\n" //
                + ("\"runtime-name\" => \"" + nameFor(contextRoot) + "\",\n") //
                + "\"subdeployment\" => undefined,\n" //
                + "\"subsystem\" => {\"web\" => {\n" //
                + ("\"context-root\" => \"/" + contextRoot + "\",\n") //
                + "\"virtual-host\" => \"default-host\",\n" //
                + "\"servlet\" => {\"javax.ws.rs.core.Application\" => {\n" //
                + "\"servlet-class\" => \"org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher\",\n" //
                + "\"servlet-name\" => \"javax.ws.rs.core.Application\"\n" //
                + "}}\n" //
                + "}}\n" //
                + "}";
    }

    public static void givenDeployments(Repository repository, ContextRoot... contextRoots) {
        for (ContextRoot contextRoot : contextRoots) {
            for (Version version : fakeVersionsFor(contextRoot)) {
                CheckSum checksum = fakeChecksumFor(contextRoot, version);
                when(repository.getByChecksum(checksum)).thenReturn(deploymentFor(contextRoot, version));
                when(repository.getArtifactInputStream(checksum)) //
                        .thenReturn(inputStreamFor(contextRoot, version));
            }
        }
    }

    public static void givenDeployments(final Container container, ContextRoot... contextRoots) {
        final List<Deployment> deployments = new ArrayList<>();
        for (ContextRoot contextRoot : contextRoots) {
            Deployment deployment = deploymentFor(contextRoot);
            givenDeployment(container, deployments, deployment);
        }
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Deployment deployment = invocation.getArgumentAt(0, Deployment.class);
                Deployment deploymentIs = deploymentFrom(invocation.getArgumentAt(1, InputStream.class));
                assert deployment.equals(deploymentIs);
                givenDeployment(container, deployments, deployment);
                return null;
            }
        }).when(container).deploy(any(Deployment.class), any(InputStream.class));
        when(container.getAllDeployments()).thenReturn(deployments);
    }

    public static void givenDeployment(Container container, List<Deployment> deployments, Deployment deployment) {
        ContextRoot contextRoot = deployment.getContextRoot();
        when(container.getDeploymentWith(contextRoot)).thenReturn(deployment);
        when(container.hasDeploymentWith(contextRoot)).thenReturn(true);
        when(container.getDeploymentWith(deployment.getCheckSum())).thenReturn(deployment);
        deployments.add(deployment);
    }

    @SneakyThrows(IOException.class)
    public static void givenDeployments(ModelControllerClient client, ContextRoot... contextRoots) {
        when(client.execute(eq(readAllDeploymentsCli()), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(successCli(readDeploymentsCliResult(contextRoots))));
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

    public static Form deploymentForm(String action, ContextRoot contextRoot, Version version) {
        return new Form("action", action) //
                .param("checkSum", fakeChecksumFor(contextRoot, version).toString()) //
                .param("contextRoot", contextRoot.getValue());
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
