package com.github.t1.deployer;

import static com.github.t1.deployer.ArtifactoryMock.*;
import static com.github.t1.deployer.TestData.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContainerTest {
    private static final Version NO_VERSION = null;

    @InjectMocks
    Container container;
    @Mock
    ModelControllerClient client;
    @Mock
    Repository repository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private void givenDeployments(ContextRoot... deploymentNames) {
        TestData.givenDeployments(repository, deploymentNames);
        TestData.givenDeployments(client, deploymentNames);
    }

    @Test
    public void shouldFailToGetDeploymentByUnknownContextRoot() throws IOException {
        when(client.execute(eq(readAllDeploymentsCli()), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(successCli("[]")));

        expectedException.expect(WebException.class);
        expectedException.expectMessage("no deployment with context root [unknown]");

        container.getDeploymentByContextRoot(new ContextRoot("unknown"));
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
    public void shouldGetOneDeployment() {
        givenDeployments(FOO);

        Deployment deployment = container.getDeploymentByContextRoot(FOO);

        assertDeployment(FOO, NO_VERSION, deployment);
    }

    @Test
    public void shouldGetOneOfTwoDeployments() {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentByContextRoot(FOO);

        assertDeployment(FOO, NO_VERSION, deployment);
    }

    @Test
    public void shouldGetTheOtherOfTwoDeployments() {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentByContextRoot(BAR);

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
