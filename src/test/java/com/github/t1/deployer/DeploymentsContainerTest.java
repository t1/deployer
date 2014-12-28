package com.github.t1.deployer;

import static com.github.t1.deployer.DeploymentsContainer.*;
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
public class DeploymentsContainerTest {
    @InjectMocks
    DeploymentsContainer container;
    @Mock
    ModelControllerClient client;
    @Mock
    Repository repository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldFailToGetDeploymentByUnknownContextRoot() throws IOException {
        String notFound = "JBAS014807: Management resource '[(\\\"deployment\\\" => \\\"unknown.war\\\")]' not found\"";
        when(client.execute(eq(readDeploymentModel("unknown.war")), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(failed(notFound)));

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("JBAS014807");
        expectedException.expectMessage("unknown");

        container.getDeploymentByContextRoot("unknown");
    }

    @Test
    public void shouldFailToGetDeploymentFromMissingContainer() throws IOException {
        when(client.execute(eq(readDeploymentModel("*")), any(OperationMessageHandler.class))) //
                .thenThrow(new IOException("dummy"));

        expectedException.expect(IOException.class);
        expectedException.expectMessage("dummy");

        container.getAllDeployments();
    }

    @Test
    public void shouldGetOneDeployment() {
        givenDeployments(client, repository, FOO);

        Deployment deployment = container.getDeploymentByContextRoot(FOO);

        assertDeployment(FOO, deployment);
    }

    @Test
    public void shouldGetOneOfTwoDeployments() {
        givenDeployments(client, repository, FOO, BAR);

        Deployment deployment = container.getDeploymentByContextRoot(FOO);

        assertDeployment(FOO, deployment);
    }

    @Test
    public void shouldGetTheOtherOfTwoDeployments() {
        givenDeployments(client, repository, FOO, BAR);

        Deployment deployment = container.getDeploymentByContextRoot(BAR);

        assertDeployment(BAR, deployment);
    }

    @Test
    public void shouldGetAllDeployments() {
        givenDeployments(client, repository, FOO, BAR);

        List<Deployment> deployments = container.getAllDeployments();

        assertEquals(2, deployments.size());
        assertDeployment(FOO, deployments.get(0));
        assertDeployment(BAR, deployments.get(1));
    }
}
