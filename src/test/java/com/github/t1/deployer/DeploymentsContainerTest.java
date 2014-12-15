package com.github.t1.deployer;

import static com.github.t1.deployer.DeploymentsContainer.*;
import static com.github.t1.deployer.TestData.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.client.*;
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
    VersionsGateway versionsGateway;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private void givenDeployments(String... deployments) throws IOException {
        givenReadDeploymentsInClientReturns(success(deployments(deployments)));
        for (String deployment : deployments) {
            when(versionsGateway.searchByChecksum(checksumFor(deployment))).thenReturn(versionFor(deployment));
        }
    }

    private void givenReadDeploymentsInClientReturns(String response) throws IOException {
        givenReadDeploymentsReturns(client, response);
    }

    @Test
    public void shouldFailToGetDeploymentByUnknownContextRoot() throws IOException {
        givenReadDeploymentsInClientReturns(failed("JBAS014807: Management resource '[(\\\"deployment\\\" => \\\"unknown\\\")]' not found\""));

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("JBAS014807");
        expectedException.expectMessage("unknown");

        container.getDeploymentByContextRoot("unknown");
    }

    @Test
    public void shouldFailToGetDeploymentFromMissingContainer() throws IOException {
        when(client.execute(eq(READ_DEPLOYMENTS), any(OperationMessageHandler.class))) //
                .thenThrow(new IOException("dummy"));

        expectedException.expect(IOException.class);
        expectedException.expectMessage("dummy");

        container.getDeploymentByContextRoot(FOO);
    }

    @Test
    public void shouldReadOneDeployment() throws IOException {
        givenDeployments(FOO);

        Deployment deployment = container.getDeploymentByContextRoot(FOO);

        assertDeployment(FOO, deployment);
    }

    @Test
    public void shouldReadOneOfTwoDeployments() throws IOException {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentByContextRoot(FOO);

        assertDeployment(FOO, deployment);
    }

    @Test
    public void shouldReadTheOtherOfTwoDeployments() throws IOException {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentByContextRoot(BAR);

        assertDeployment(BAR, deployment);
    }

    @Test
    public void shouldGetAllDeployments() throws IOException {
        givenDeployments(FOO, BAR);

        List<Deployment> deployments = container.getAllDeployments();

        assertEquals(2, deployments.size());
        assertDeployment(FOO, deployments.get(0));
        assertDeployment(BAR, deployments.get(1));
    }
}
