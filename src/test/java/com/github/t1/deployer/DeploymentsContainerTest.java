package com.github.t1.deployer;

import static com.github.t1.deployer.DeploymentsContainer.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

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
    DeploymentsContainer deployments;
    @Mock
    ModelControllerClient client;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldFailToGetDeploymentByUnknownContextRoot() throws IOException {
        when(client.execute(eq(READ_DEPLOYMENTS), any(OperationMessageHandler.class)))
                .thenReturn(
                        ModelNode
                                .fromString("{\"outcome\" => \"failed\",\n" //
                                        + "\"failure-description\" => \"JBAS014807: Management resource '[(\\\"deployment\\\" => \\\"unknown\\\")]' not found\"\n" //
                                        + "}\n"));

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("JBAS014807");
        expectedException.expectMessage("unknown");

        deployments.getDeploymentByContextRoot("unknown");
    }
}
