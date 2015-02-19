package com.github.t1.deployer;

import static com.github.t1.deployer.ArtifactoryMock.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;

import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import lombok.SneakyThrows;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

public class TestData {
    public static Deployment deploymentFor(ContextRoot contextRoot, Version version) {
        Deployment deployment = new Deployment(nameFor(contextRoot), contextRoot, checksumFor(contextRoot, version));
        deployment.setVersion(version);
        return deployment;
    }

    public static DeploymentName nameFor(ContextRoot contextRoot) {
        return new DeploymentName(contextRoot + ".war");
    }

    public static Deployment deploymentFor(ContextRoot contextRoot) {
        return deploymentFor(contextRoot, versionFor(contextRoot));
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
                + checksumFor(contextRoot).hexByteArray() //
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
            for (Version version : availableVersionsFor(contextRoot)) {
                CheckSum checksum = checksumFor(contextRoot, version);
                when(repository.getByChecksum(checksum)).thenReturn(deploymentFor(contextRoot, version));
                when(repository.getArtifactInputStream(checksum)) //
                        .thenReturn(inputStreamFor(contextRoot, version));
            }
        }
    }

    public static void givenDeployments(Container container, ContextRoot... contextRoots) {
        List<Deployment> deployments = new ArrayList<>();
        for (ContextRoot contextRoot : contextRoots) {
            Deployment deployment = deploymentFor(contextRoot);
            deployments.add(deployment);
            when(container.getDeploymentByContextRoot(contextRoot)).thenReturn(deployment);
        }
        when(container.getAllDeployments()).thenReturn(deployments);
    }

    @SneakyThrows(IOException.class)
    public static void givenDeployments(ModelControllerClient client, ContextRoot... contextRoots) {
        when(client.execute(eq(readAllDeploymentsCli()), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(successCli(readDeploymentsCliResult(contextRoots))));
    }

    public static String deploymentJson(ContextRoot contextRoot) {
        return deploymentJson(contextRoot, versionFor(contextRoot));
    }

    public static String deploymentJson(ContextRoot contextRoot, Version version) {
        return "{" //
                + "\"name\":\"" + nameFor(contextRoot) + "\"," //
                + "\"contextRoot\":\"" + contextRoot + "\"," //
                + "\"checkSum\":\"" + checksumFor(contextRoot, version) + "\"," //
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
        assertDeployment(contextRoot, versionFor(contextRoot), deployment);
    }

    public static void assertDeployment(ContextRoot contextRoot, Version expectedVersion, Deployment deployment) {
        assertEquals(contextRoot, deployment.getContextRoot());
        assertEquals(nameFor(contextRoot), deployment.getName());
        assertEquals(expectedVersion, deployment.getVersion());
    }
}
