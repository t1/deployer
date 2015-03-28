package com.github.t1.deployer.container;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import lombok.SneakyThrows;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.TestData;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.deployer.tools.WebException;

@RunWith(MockitoJUnitRunner.class)
public class ContainerDeploymentsTest {
    private static final Version NO_VERSION = null;

    @InjectMocks
    Container container;
    @Mock
    ModelControllerClient client;
    @Mock
    Repository repository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
            container.getDeploymentWith(new ContextRoot("unknown"));
            fail("WebException expected");
        } catch (WebException e) {
            assertEquals(NOT_FOUND, e.getResponse().getStatusInfo());
            assertEquals("no deployment with context root [unknown]", e.getResponse().getEntity());
        }
    }

    @Test
    public void shouldFailToGetDeploymentFromFailingContainer() throws IOException {
        when(client.execute(eq(readAllDeploymentsCli()), any(OperationMessageHandler.class))) //
                .thenThrow(new IOException("dummy"));

        expectedException.expect(IOException.class);
        expectedException.expectMessage("dummy");

        container.getAllDeployments();
    }

    @Test
    public void shouldGetNoDeployment() {
        givenDeployments();

        boolean exists = container.hasDeploymentWith(FOO);

        assertFalse(exists);
    }

    @Test
    public void shouldGetOneDeployment() {
        givenDeployments(FOO);

        Deployment deployment = container.getDeploymentWith(FOO);

        assertDeployment(FOO, NO_VERSION, deployment);
    }

    @Test
    public void shouldGetOneOfTwoDeployments() {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentWith(FOO);

        assertDeployment(FOO, NO_VERSION, deployment);
    }

    @Test
    public void shouldGetTheOtherOfTwoDeployments() {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentWith(BAR);

        assertDeployment(BAR, NO_VERSION, deployment);
    }

    @Test
    public void shouldGetAllDeployments() {
        givenDeployments(FOO, BAR);

        List<Deployment> deployments = container.getAllDeployments();

        assertEquals(2, deployments.size());
        assertDeployment(FOO, NO_VERSION, deployments.get(0));
        assertDeployment(BAR, NO_VERSION, deployments.get(1));
    }
}
