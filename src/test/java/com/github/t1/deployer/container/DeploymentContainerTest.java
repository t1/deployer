package com.github.t1.deployer.container;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.TestData;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.ramlap.tools.ProblemDetail;

import lombok.SneakyThrows;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentContainerTest {
    private static final Version NO_VERSION = null;

    @InjectMocks
    DeploymentContainer container;
    @Mock
    ModelControllerClient client;
    @Mock
    Repository repository;

    @SneakyThrows(IOException.class)
    private void givenDeployments(ContextRoot... contextRoots) {
        TestData.givenDeployments(repository, contextRoots);

        when(client.execute(eq(readAllDeploymentsCli()), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(successCli(readDeploymentsCliResult(contextRoots))));
    }

    private static ModelNode readAllDeploymentsCli() {
        ModelNode node = new ModelNode();
        node.get("address").add("deployment", "*");
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    public static void assertStatusDetails(Status status, String detail, WebApplicationException e) {
        Response response = e.getResponse();
        assertThat(response.getStatusInfo()).isEqualTo(status);
        ProblemDetail error = (ProblemDetail) response.getEntity();
        assertThat(error.status()).isEqualTo(status);
        assertThat(error.detail()).isEqualTo(detail);
    }

    public static String readDeploymentsCliResult(ContextRoot... contextRoots) {
        StringBuilder out = new StringBuilder();
        out.append("[");
        for (ContextRoot contextRoot : contextRoots) {
            if (out.length() > 1)
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

    @Test
    public void shouldFailToGetDeploymentByUnknownContextRoot() throws IOException {
        when(client.execute(eq(readAllDeploymentsCli()), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(successCli("[]")));

        try {
            container.getDeploymentFor(new ContextRoot("unknown"));
            fail("WebException expected");
        } catch (WebApplicationException e) {
            assertStatusDetails(NOT_FOUND, "no deployment with context root [unknown]", e);
        }
    }

    @Test
    public void shouldFailToGetDeploymentFromFailingContainer() throws IOException {
        when(client.execute(eq(readAllDeploymentsCli()), any(OperationMessageHandler.class))) //
                .thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> container.getAllDeployments()) //
                .isInstanceOf(IOException.class) //
                .hasMessage("dummy");
    }

    @Test
    public void shouldGetNoDeployment() {
        givenDeployments();

        boolean exists = container.hasDeploymentWith(FOO);

        assertThat(exists).isFalse();
    }

    @Test
    public void shouldGetOneDeployment() {
        givenDeployments(FOO);

        Deployment deployment = container.getDeploymentFor(FOO);

        assertDeployment(FOO, NO_VERSION, deployment);
    }

    @Test
    public void shouldGetOneOfTwoDeployments() {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentFor(FOO);

        assertDeployment(FOO, NO_VERSION, deployment);
    }

    @Test
    public void shouldGetTheOtherOfTwoDeployments() {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentFor(BAR);

        assertDeployment(BAR, NO_VERSION, deployment);
    }

    @Test
    public void shouldGetAllDeployments() {
        givenDeployments(FOO, BAR);

        List<Deployment> deployments = container.getAllDeployments();

        assertThat(deployments.size()).isEqualTo(2);
        assertDeployment(FOO, NO_VERSION, deployments.get(0));
        assertDeployment(BAR, NO_VERSION, deployments.get(1));
    }
}
