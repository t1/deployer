package com.github.t1.deployer;

import static com.github.t1.deployer.DeploymentsContainer.*;
import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import lombok.SneakyThrows;

import org.hamcrest.*;
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

    public static final String FOO_CHECKSUM = "32D59F10CCEA21A7844D66C9DBED030FD67964D1";
    public static final String BAR_CHECKSUM = "FBD368E959DF458C562D0A4D1F70049D0FA3D620";

    public static void givenDeployments(Repository repository, String... deploymentNames) {
        for (String name : deploymentNames) {
            when(repository.searchByChecksum(checksumFor(name))).thenReturn(versionFor(name));
            for (Version version : availableVersionsFor(name)) {
                when(repository.getArtifactInputStream(argThat(isDeploymentFor(name, version)))) //
                        .thenReturn(inputStreamFor(name, version));
            }
        }
    }

    public static Matcher<Deployment> isDeploymentFor(final String name, final Version version) {
        return new BaseMatcher<Deployment>() {
            @Override
            public boolean matches(Object item) {
                if (!(item instanceof Deployment))
                    return false;
                Deployment deployment = (Deployment) item;
                return (name + ".war").equals(deployment.getName()) //
                        && name.equals(deployment.getContextRoot()) //
                        // ignore hash code, as this is, e.g., not sent by the client
                        && version.equals(deployment.getVersion()) //
                ;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a deployment for " + name + " at version " + version);
            }
        };
    }

    public static void givenDeployments(DeploymentsContainer container, String... contextRoots) {
        List<Deployment> deployments = new ArrayList<>();
        for (String contextRoot : contextRoots) {
            Deployment deployment = new Deployment(contextRoot + ".war", contextRoot, checksumFor(contextRoot));
            deployments.add(deployment);
            when(container.getDeploymentByContextRoot(contextRoot)).thenReturn(deployment);
        }
        when(container.getAllDeployments()).thenReturn(deployments);
    }

    @SneakyThrows(IOException.class)
    public static void givenDeployments(ModelControllerClient client, String... deploymentNames) {
        StringBuilder all = new StringBuilder();
        for (String name : deploymentNames) {
            if (all.length() == 0)
                all.append("[");
            else
                all.append(",");
            all.append("{\n" //
                    + "\"address\" => [(\"deployment\" => \"" + name + ".war\")],\n" //
                    + "\"outcome\" => \"success\",\n" //
                    + "\"result\" => " + deployment(name) + "\n" //
                    + "}\n");
            when(client.execute(eq(readDeploymentModel(name + ".war")), any(OperationMessageHandler.class))) //
                    .thenReturn(ModelNode.fromString(success(deployment(name))));
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

    public static Version versionFor(String name) {
        switch (name) {
            case FOO:
                return CURRENT_FOO_VERSION;
            case BAR:
                return CURRENT_BAR_VERSION;
            default:
                throw new IllegalArgumentException("no test data version defined for " + name);
        }
    }

    public static List<Version> availableVersionsFor(String name) {
        switch (name) {
            case FOO:
                return FOO_VERSIONS;
            case BAR:
                return BAR_VERSIONS;
            default:
                throw new IllegalArgumentException("no test data available versions defined for " + name);
        }
    }

    public static InputStream inputStreamFor(String name, Version version) {
        return new StringInputStream(name + "-content@" + version);
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

    public static Entity<String> entity(String contextRoot, Version version) {
        return Entity.json("{\n" //
                + "   \"name\" : \"" + contextRoot + ".war\",\n" //
                + "   \"version\" : \"" + version + "\",\n" //
                + "   \"contextRoot\" : \"" + contextRoot + "\"\n" //
                + "}");
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
}
