package com.github.t1.deployer;

import static com.github.t1.deployer.DeploymentsContainer.*;
import static java.util.Arrays.*;
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

public class TestData {
    private static final class StringInputStream extends ByteArrayInputStream {
        private final String string;

        private StringInputStream(String string) {
            super(string.getBytes());
            this.string = string;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            StringInputStream that = (StringInputStream) obj;
            return this.string.equals(that.string);
        }

        @Override
        public int hashCode() {
            return string.hashCode();
        }

        @Override
        public String toString() {
            return "[" + string + "]";
        }
    }

    public static final String FOO = "foo";
    public static final String BAR = "bar";

    public static final Version NEWEST_FOO_VERSION = new Version("1.3.10");
    public static final Version CURRENT_FOO_VERSION = new Version("1.3.1");

    public static final List<Version> FOO_VERSIONS = asList(//
            NEWEST_FOO_VERSION, //
            new Version("1.3.2"), //
            CURRENT_FOO_VERSION, //
            new Version("1.3.0"), //
            new Version("1.2.1"), //
            new Version("1.2.1-SNAPSHOT"), //
            new Version("1.2.0") //
            );

    public static final Version CURRENT_BAR_VERSION = new Version("0.3");

    public static final List<Version> BAR_VERSIONS = asList(//
            CURRENT_BAR_VERSION, //
            new Version("0.2") //
            );

    public static Deployment deploymentFor(String contextRoot) {
        return deploymentFor(contextRoot, versionFor(contextRoot));
    }

    public static Deployment deploymentFor(String contextRoot, Version version) {
        Deployment deployment = new Deployment(nameFor(contextRoot), contextRoot, checksumFor(contextRoot));
        deployment.setVersion(version);
        return deployment;
    }

    public static void givenDeployments(Repository repository, String... deploymentNames) {
        for (String name : deploymentNames) {
            when(repository.getVersionByChecksum(checksumFor(name))).thenReturn(versionFor(name));
            for (Version version : availableVersionsFor(name)) {
                when(repository.getArtifactInputStream(checksumFor(name, version))) //
                        .thenReturn(inputStreamFor(name, version));
            }
        }
    }

    public static void givenDeployments(DeploymentsContainer container, String... contextRoots) {
        List<Deployment> deployments = new ArrayList<>();
        for (String contextRoot : contextRoots) {
            Deployment deployment = deploymentFor(contextRoot);
            deployments.add(deployment);
            when(container.getDeploymentByContextRoot(contextRoot)).thenReturn(deployment);
        }
        when(container.getAllDeployments()).thenReturn(deployments);
    }

    @SneakyThrows(IOException.class)
    public static void givenDeployments(ModelControllerClient client, String... deploymentNames) {
        StringBuilder all = new StringBuilder();
        for (String contextRoot : deploymentNames) {
            if (all.length() == 0)
                all.append("[");
            else
                all.append(",");
            all.append("{\n" //
                    + "\"address\" => [(\"deployment\" => \"" + nameFor(contextRoot) + "\")],\n" //
                    + "\"outcome\" => \"success\",\n" //
                    + "\"result\" => " + deploymentCli(contextRoot) + "\n" //
                    + "}\n");
            when(client.execute(eq(readDeploymentModel(nameFor(contextRoot))), any(OperationMessageHandler.class))) //
                    .thenReturn(ModelNode.fromString(successCli(deploymentCli(contextRoot))));
        }
        all.append("]");
        when(client.execute(eq(readDeploymentModel("*")), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(successCli(all.toString())));
    }

    public static CheckSum checksumFor(String name) {
        return checksumFor(name, versionFor(name));
    }

    public static CheckSum checksumFor(String name, Version version) {
        return CheckSum.of(("md5(" + name + "@" + version + ")").getBytes());
    }

    public static String nameFor(String contextRoot) {
        return contextRoot + ".war";
    }

    public static Version versionFor(String name) {
        switch (name) {
            case FOO:
                return CURRENT_FOO_VERSION;
            case BAR:
                return CURRENT_BAR_VERSION;
            default:
                throw new IllegalArgumentException("no test data 'version' defined for " + name);
        }
    }

    public static List<Version> availableVersionsFor(String name) {
        switch (name) {
            case FOO:
                return FOO_VERSIONS;
            case BAR:
                return BAR_VERSIONS;
            default:
                throw new IllegalArgumentException("no test data 'available versions' defined for " + name);
        }
    }

    public static InputStream inputStreamFor(String name, Version version) {
        return new StringInputStream(name + "-content@" + version);
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

    public static String[] deploymentsCli(String... deployments) {
        String[] result = new String[deployments.length];
        for (int i = 0; i < deployments.length; i++) {
            result[i] = deploymentCli(deployments[i]);
        }
        return result;
    }

    public static String deploymentCli(String contextRoot) {
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

    public static String deploymentJson(String contextRoot) {
        return deploymentJson(contextRoot, versionFor(contextRoot));
    }

    public static String deploymentJson(String contextRoot, Version version) {
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

    public static void assertDeployment(String contextRoot, Deployment deployment) {
        assertDeployment(contextRoot, versionFor(contextRoot), deployment);
    }

    public static void assertDeployment(String contextRoot, Version expectedVersion, Deployment deployment) {
        assertEquals(contextRoot, deployment.getContextRoot());
        assertEquals(nameFor(contextRoot), deployment.getName());
        assertEquals(expectedVersion, deployment.getVersion());
    }
}
