package com.github.t1.deployer;

import static com.github.t1.deployer.TestData.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import javax.ws.rs.core.Response;

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
    DeploymentsContainer container;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final List<Deployment> installedDeployments = new ArrayList<>();

    @Before
    public void setup() {
        when(container.getAllDeployments()).thenReturn(installedDeployments);
    }

    @AllArgsConstructor
    private class OngoingDeploymentStub {
        String contextRoot;

        public void availableVersions(String... versions) {
            List<Version> list = new ArrayList<>();
            for (String version : versions) {
                list.add(new Version(version));
            }
            when(versionsGateway.searchVersions(contextRoot + "-group", contextRoot + "-artifact")).thenReturn(list);
        }
    }

    private OngoingDeploymentStub givenDeployment(String name, String contextRoot, String checksum, String version) {
        Deployment deployment = new Deployment(container, versionsGateway, name, contextRoot, checksum);
        installedDeployments.add(deployment);
        when(versionsGateway.searchByChecksum(checksum)).thenReturn(version);
        when(container.getDeploymentByContextRoot(contextRoot)).thenReturn(deployment);
        return new OngoingDeploymentStub(contextRoot);
    }

    @Test
    public void shouldAllDeployments() {
        givenDeployment("foo.war", "foo", FOO_CHECKSUM, CURRENT_FOO_VERSION).availableVersions("1.3.1");

        Response response = deployments.getAllDeployments();

        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        List<Deployment> list = (List<Deployment>) response.getEntity();
        assertEquals(1, list.size());
        Deployment deployment = list.get(0);
        assertEquals("foo.war", deployment.getName());
        assertEquals("foo", deployment.getContextRoot());
        assertEquals(CURRENT_FOO_VERSION, deployment.getVersion());
        assertEquals("[1.3.1]", deployment.getAvailableVersions().toString());
    }

    @Test
    public void shouldGetDeploymentByContextRootMatrix() {
        givenDeployment("foo.war", "foo", FOO_CHECKSUM, CURRENT_FOO_VERSION).availableVersions("1.3.1");

        Deployment deployment = deployments.getDeploymentsByContextRoot("foo");

        assertEquals("1.3.1", deployment.getVersion().toString());
        assertEquals("[1.3.1]", deployment.getAvailableVersions().toString());
    }

    @Test
    public void shouldGetDeploymentVersions() {
        givenDeployment("foo.war", "foo", FOO_CHECKSUM, CURRENT_FOO_VERSION).availableVersions("1.3.2", "1.3.1",
                "1.3.0", "1.2.8-SNAPSHOT", "1.2.7", "1.2.6");

        Deployment deployment = deployments.getDeploymentsByContextRoot("foo");

        assertEquals("1.3.1", deployment.getVersion().toString());
        assertEquals("[1.3.2, 1.3.1, 1.3.0, 1.2.8-SNAPSHOT, 1.2.7, 1.2.6]", deployment.getAvailableVersions()
                .toString());
    }
}
