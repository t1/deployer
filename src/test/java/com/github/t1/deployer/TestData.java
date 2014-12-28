package com.github.t1.deployer;

import static com.github.t1.deployer.DeploymentsContainer.*;
import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import lombok.SneakyThrows;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

public class TestData {
    public static final String FOO = "foo";
    public static final String BAR = "bar";

    public static final String CURRENT_FOO_VERSION = "1.3.1";
    public static final String CURRENT_BAR_VERSION = "0.3";

    public static final String FOO_CHECKSUM = "32D59F10CCEA21A7844D66C9DBED030FD67964D1";
    public static final String BAR_CHECKSUM = "FBD368E959DF458C562D0A4D1F70049D0FA3D620";

    @SneakyThrows(IOException.class)
    public static void givenDeployments(ModelControllerClient client, Repository repository, String... deploymentNames) {
        StringBuilder all = new StringBuilder();
        for (String name : deploymentNames) {
            if (all.length() == 0)
                all.append("[");
            else
                all.append(",");
            all.append("{\n" //
                    + "            \"address\" => [(\"deployment\" => \"" + name + ".war\")],\n" //
                    + "\"outcome\" => \"success\",\n" //
                    + "\"result\" => " + deployment(name) + "\n" //
                    + "}\n");
            when(client.execute(eq(readDeploymentModel(name + ".war")), any(OperationMessageHandler.class))) //
                    .thenReturn(ModelNode.fromString(success(deployment(name))));
            when(repository.searchByChecksum(checksumFor(name))).thenReturn(versionFor(name));
        }
        all.append("]");
        when(client.execute(eq(readDeploymentModel("*")), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(success(all.toString())));
    }

    public static String checksumFor(String name) {
        switch (name) {
            case FOO:
                return FOO_CHECKSUM;
            case BAR:
                return BAR_CHECKSUM;
            default:
                throw new IllegalArgumentException("no test data checksum defined for " + name);
        }
    }

    public static String versionFor(String name) {
        switch (name) {
            case FOO:
                return CURRENT_FOO_VERSION;
            case BAR:
                return CURRENT_BAR_VERSION;
            default:
                throw new IllegalArgumentException("no test data version defined for " + name);
        }
    }

    public static String byteArray(String checksum) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < checksum.length(); i++) {
            if (i != 0)
                out.append(", ");
            out.append("0x").append(checksum.charAt(i++)).append(checksum.charAt(i));
        }
        return out.toString();
    }

    public static final List<Version> FOO_VERSIONS = asList(//
            new Version("1.3.10"), //
            new Version("1.3.2"), //
            new Version(CURRENT_FOO_VERSION), //
            new Version("1.3.0"), //
            new Version("1.2.1"), //
            new Version("1.2.1-SNAPSHOT"), //
            new Version("1.2.0") //
            );

    public static String failed(String message) {
        return "{\"outcome\" => \"failed\",\n" //
                + "\"failure-description\" => \"" + message + "\n" //
                + "}\n";
    }

    public static String success(String result) {
        return "{\n" //
                + "\"outcome\" => \"success\",\n" //
                + "\"result\" => " + result + "\n" //
                + "}\n";
    }

    public static String[] deployments(String... deployments) {
        String[] result = new String[deployments.length];
        for (int i = 0; i < deployments.length; i++) {
            result[i] = deployment(deployments[i]);
        }
        return result;
    }

    public static String deployment(String deployment) {
        return "{\n" //
                + "\"content\" => [{\"hash\" => bytes {\n" //
                + byteArray(checksumFor(deployment)) //
                + "}}],\n" //
                + "\"enabled\" => true,\n" //
                + ("\"name\" => \"" + deployment + ".war\",\n") //
                + "\"persistent\" => true,\n" //
                + ("\"runtime-name\" => \"" + deployment + ".war\",\n") //
                + "\"subdeployment\" => undefined,\n" //
                + "\"subsystem\" => {\"web\" => {\n" //
                + ("\"context-root\" => \"/" + deployment + "\",\n") //
                + "\"virtual-host\" => \"default-host\",\n" //
                + "\"servlet\" => {\"javax.ws.rs.core.Application\" => {\n" //
                + "\"servlet-class\" => \"org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher\",\n" //
                + "\"servlet-name\" => \"javax.ws.rs.core.Application\"\n" //
                + "}}\n" //
                + "}}\n" //
                + "}";
    }

    public static void assertDeployment(String name, Deployment deployment) {
        assertEquals(name, deployment.getContextRoot());
        assertEquals(name + ".war", deployment.getName());
        assertEquals(versionFor(name), deployment.getVersion());
    }
}
