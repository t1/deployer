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
    Repository repository;
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
        String contextRoot, version;

        public void availableVersions(String... versions) {
            List<Version> list = new ArrayList<>();
            for (String version : versions) {
                list.add(new Version(version));
            }
            Deployment deployment = new Deployment(contextRoot + ".war", contextRoot, checksumFor(contextRoot));
            deployment.setVersion(version);
            when(repository.availableVersionsFor(deployment)).thenReturn(list);
        }
    }

    private OngoingDeploymentStub givenDeployment(String name, String contextRoot, String checksum, String version) {
        Deployment deployment = new Deployment(name, contextRoot, checksum);
        installedDeployments.add(deployment);
        when(repository.searchByChecksum(checksum)).thenReturn(version);
        when(container.getDeploymentByContextRoot(contextRoot)).thenReturn(deployment);
        return new OngoingDeploymentStub(contextRoot, version);
    }

    @Test
    public void shouldGetAllDeployments() {
        givenDeployment("foo.war", FOO, FOO_CHECKSUM, CURRENT_FOO_VERSION);
        givenDeployment("bar.war", BAR, BAR_CHECKSUM, CURRENT_BAR_VERSION);

        Response response = deployments.getAllDeployments();

        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        List<Deployment> list = (List<Deployment>) response.getEntity();
        assertEquals(2, list.size());

        Deployment deployment0 = list.get(0);
        assertEquals("foo.war", deployment0.getName());
        assertEquals("foo", deployment0.getContextRoot());
        assertEquals(CURRENT_FOO_VERSION, deployment0.getVersion());

        Deployment deployment1 = list.get(1);
        assertEquals("bar.war", deployment1.getName());
        assertEquals("bar", deployment1.getContextRoot());
        assertEquals(CURRENT_BAR_VERSION, deployment1.getVersion());
    }

    @Test
    public void shouldGetDeploymentByContextRootMatrix() {
        givenDeployment("foo.war", "foo", FOO_CHECKSUM, CURRENT_FOO_VERSION).availableVersions("1.3.1");

        DeploymentResource deployment = deployments.getDeploymentsByContextRoot("foo");

        assertEquals("1.3.1", deployment.getVersion().toString());
        assertEquals("[1.3.1]", deployment.getAvailableVersions().toString());
    }

    @Test
    public void shouldGetDeploymentVersions() {
        givenDeployment("foo.war", "foo", FOO_CHECKSUM, CURRENT_FOO_VERSION).availableVersions("1.3.2", "1.3.1",
                "1.3.0", "1.2.8-SNAPSHOT", "1.2.7", "1.2.6");

        DeploymentResource deployment = deployments.getDeploymentsByContextRoot("foo");

        assertEquals("1.3.1", deployment.getVersion().toString());
        assertEquals("[1.3.2, 1.3.1, 1.3.0, 1.2.8-SNAPSHOT, 1.2.7, 1.2.6]", deployment.getAvailableVersions()
                .toString());
    }
}
