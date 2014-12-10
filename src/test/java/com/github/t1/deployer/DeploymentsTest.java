package com.github.t1.deployer;

import static com.github.t1.deployer.TestData.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import lombok.AllArgsConstructor;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentsTest {
    @InjectMocks
    Deployments deployments;
    @Mock
    VersionsGateway versionsGateway;
    @Mock
    DeploymentContainer deploymentsInfo;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final List<Deployment> installedDeployments = new ArrayList<>();

    @Before
    public void setup() {
        when(deploymentsInfo.getAllDeployments()).thenReturn(installedDeployments);
    }

    @AllArgsConstructor
    private class OngoingDeploymentStub {
        String contextRoot;

        public void availableVersions(String... versions) {
            List<Version> list = new ArrayList<>();
            for (String version : versions) {
                list.add(new Version(version));
            }
            String stripped = contextRoot.substring(1);
            when(versionsGateway.searchVersions(stripped + "-group", stripped + "-artifact")).thenReturn(list);
        }
    }

    private OngoingDeploymentStub givenDeployment(String contextRoot, String checksum, Version version) {
        assertTrue(contextRoot.startsWith("/"));
        Deployment deployment = new Deployment(versionsGateway, contextRoot, checksum);
        installedDeployments.add(deployment);
        when(versionsGateway.searchByChecksum(checksum)).thenReturn(version);
        when(deploymentsInfo.getDeploymentByContextRoot(contextRoot)).thenReturn(deployment);
        return new OngoingDeploymentStub(contextRoot);
    }

    @Test
    public void shouldGetDeploymentByContextRoot() {
        givenDeployment("/foo", FOO_CHECKSUM, CURRENT_FOO_VERSION);

        Deployment deployment = (Deployment) deployments.getDeploymentsByContextRoot("/foo").getEntity();

        assertEquals("1.3.1", deployment.getVersion().toString());
    }

    @Test
    public void shouldReadVersionsFromArtifactory() {
        givenDeployment("/foo", FOO_CHECKSUM, CURRENT_FOO_VERSION).availableVersions("1.3.2", "1.3.1", "1.3.0",
                "1.2.8-SNAPSHOT", "1.2.7", "1.2.6");

        Deployment deployment = (Deployment) deployments.getDeploymentsByContextRoot("/foo").getEntity();

        assertEquals("1.3.1", deployment.getVersion().toString());
        // assertEquals("[1.3.2, 1.3.1, 1.3.0, 1.2.8-SNAPSHOT, 1.2.7, 1.2.6]", response.toString());
    }
}
