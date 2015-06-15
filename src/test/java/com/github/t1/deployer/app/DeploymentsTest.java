package com.github.t1.deployer.app;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.glassfish.hk2.api.Factory;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.TestData.OngoingDeploymentStub;
import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.deployer.tools.FactoryInstance;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentsTest {
    @InjectMocks
    Deployments deployments;
    @Mock
    Repository repository;
    @Mock
    DeploymentContainer container;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final List<Deployment> installedDeployments = new ArrayList<>();

    @Before
    public void setup() {
        when(container.getAllDeployments()).thenReturn(installedDeployments);
        deployments.deploymentResources = new FactoryInstance<>(new Factory<DeploymentResource>() {
            @Override
            public DeploymentResource provide() {
                DeploymentResource result = new DeploymentResource();
                result.container = container;
                result.repository = repository;
                return result;
            }

            @Override
            public void dispose(DeploymentResource instance) {}
        });
    }

    private OngoingDeploymentStub givenDeployment(ContextRoot contextRoot) {
        Deployment deployment = deploymentFor(contextRoot);
        installedDeployments.add(deployment);
        when(repository.getByChecksum(fakeChecksumFor(contextRoot))).thenReturn(deploymentFor(contextRoot));
        when(container.getDeploymentFor(contextRoot)).thenReturn(deployment);
        return new OngoingDeploymentStub(repository, deploymentFor(contextRoot, fakeVersionFor(contextRoot)));
    }

    private void assertVersions(ContextRoot contextRoot, List<VersionInfo> actual, String... versions) {
        assertEquals(versions.length, actual.size());
        int i = 0;
        for (VersionInfo entry : actual) {
            Version expected = new Version(versions[i]);
            assertEquals("version#" + i, expected, entry.getVersion());
            assertEquals("checkSum#" + i, fakeChecksumFor(contextRoot, expected), entry.getCheckSum());
            i++;
        }
    }

    @Test
    public void shouldGetAllDeployments() {
        givenDeployment(FOO);
        givenDeployment(BAR);

        List<Deployment> list = deployments.getAllDeployments();

        assertEquals(2, list.size());
        assertDeployment(FOO, list.get(0));
        assertDeployment(BAR, list.get(1));
    }

    @Test
    public void shouldGetDeploymentByContextRootMatrix() {
        givenDeployment(FOO).availableVersions("1.3.1");

        DeploymentResource deployment = deployments.deploymentSubResourceByContextRoot(FOO);

        assertEquals("1.3.1", deployment.getVersion().toString());
        assertVersions(deployment.getContextRoot(), deployment.getAvailableVersions(), "1.3.1");
    }

    @Test
    public void shouldGetDeploymentVersions() {
        String[] versions = { "1.2.6", "1.2.7", "1.2.8-SNAPSHOT", "1.3.0", "1.3.1", "1.3.2" };
        givenDeployment(FOO).availableVersions(versions);

        DeploymentResource deployment = deployments.deploymentSubResourceByContextRoot(FOO);

        assertVersions(FOO, deployment.getAvailableVersions(), versions);
    }
}
