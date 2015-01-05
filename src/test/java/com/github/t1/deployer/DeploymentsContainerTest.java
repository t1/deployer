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

    private void givenDeployments(String... deploymentNames) {
        TestData.givenDeployments(repository, deploymentNames);
        TestData.givenDeployments(client, deploymentNames);
    }

    private static void assertDeployment(String name, Deployment deployment) {
        assertEquals(name, deployment.getContextRoot());
        assertEquals(name + ".war", deployment.getName());
    }

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
        givenDeployments(FOO);

        Deployment deployment = container.getDeploymentByContextRoot(FOO);

        assertDeployment(FOO, deployment);
    }

    @Test
    public void shouldGetOneOfTwoDeployments() {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentByContextRoot(FOO);

        assertDeployment(FOO, deployment);
    }

    @Test
    public void shouldGetTheOtherOfTwoDeployments() {
        givenDeployments(FOO, BAR);

        Deployment deployment = container.getDeploymentByContextRoot(BAR);

        assertDeployment(BAR, deployment);
    }

    @Test
    public void shouldGetAllDeployments() {
        givenDeployments(FOO, BAR);

        List<Deployment> deployments = container.getAllDeployments();

        assertEquals(2, deployments.size());
        assertDeployment(FOO, deployments.get(0));
        assertDeployment(BAR, deployments.get(1));
    }
}
