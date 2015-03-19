package com.github.t1.deployer;

import static com.github.t1.deployer.ArtifactoryMock.*;
import static com.github.t1.deployer.TestData.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXB;

import lombok.AllArgsConstructor;

import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.hk2.api.Factory;
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
    Container container;

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
                result.audit = null;
                result.deploymentsList = null;
                return result;
            }

            @Override
            public void dispose(DeploymentResource instance) {}
        });
    }

    @AllArgsConstructor
    private class OngoingDeploymentStub {
        Deployment deployment;

        public void availableVersions(String... versions) {
            List<Deployment> list = new ArrayList<>();
            for (String version : versions) {
                list.add(deploymentFor(deployment.getContextRoot(), new Version(version)));
            }
            when(repository.availableVersionsFor(deployment.getCheckSum())).thenReturn(list);
        }
    }

    private OngoingDeploymentStub givenDeployment(ContextRoot contextRoot) {
        Deployment deployment = deploymentFor(contextRoot);
        installedDeployments.add(deployment);
        when(repository.getByChecksum(fakeChecksumFor(contextRoot))).thenReturn(deploymentFor(contextRoot));
        when(container.getDeploymentWith(contextRoot)).thenReturn(deployment);
        return new OngoingDeploymentStub(deploymentFor(contextRoot, fakeVersionFor(contextRoot)));
    }

    private static String deploymentXml(ContextRoot contextRoot) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" //
                + "<deployment>\n" //
                + "    <name>" + nameFor(contextRoot) + "</name>\n" //
                + "    <contextRoot>" + contextRoot + "</contextRoot>\n" //
                // base64 as the @XmlSchemaType(name = "hexBinary") doesn't seem to work
                + "    <checkSum>" + fakeChecksumFor(contextRoot).base64() + "</checkSum>\n" //
                + "    <version>" + fakeVersionFor(contextRoot) + "</version>\n" //
                + "</deployment>\n";
    }

    @Test
    public void shouldMarshalDeploymentAsJson() throws Exception {
        Deployment deployment = deploymentFor(FOO);

        String json = new ObjectMapper().writeValueAsString(deployment);

        assertEquals(deploymentJson(FOO), json);
    }

    @Test
    public void shouldUnmarshalDeploymentFromJson() throws Exception {
        String json = deploymentJson(FOO);

        Deployment deployment = new ObjectMapper().readValue(json, Deployment.class);

        assertDeployment(FOO, deployment);
    }

    @Test
    public void shouldMarshalDeploymentAsXml() {
        Deployment deployment = deploymentFor(FOO);
        StringWriter xml = new StringWriter();

        JAXB.marshal(deployment, xml);

        assertEquals(deploymentXml(FOO), xml.toString());
    }

    @Test
    public void shouldUnmarshalDeploymentFromXml() {
        String xml = deploymentXml(FOO);

        Deployment deployment = JAXB.unmarshal(new StringReader(xml), Deployment.class);

        assertDeployment(FOO, deployment);
    }

    @Test
    public void shouldGetAllDeployments() {
        givenDeployment(FOO);
        givenDeployment(BAR);

        Response response = deployments.getAllDeployments();

        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        List<Deployment> list = (List<Deployment>) response.getEntity();
        assertEquals(2, list.size());
        assertDeployment(FOO, list.get(0));
        assertDeployment(BAR, list.get(1));
    }

    @Test
    public void shouldGetDeploymentByContextRootMatrix() {
        givenDeployment(FOO).availableVersions("1.3.1");

        DeploymentResource deployment = deployments.deploymentSubResourceByContextRoot(FOO);

        assertEquals("1.3.1", deployment.getVersion().toString());
        assertEquals(1, deployment.getAvailableVersions().size());
        assertEquals("1.3.1", deployment.getAvailableVersions().get(0).getVersion().toString());
    }

    @Test
    public void shouldGetDeploymentVersions() {
        givenDeployment(FOO).availableVersions("1.3.2", "1.3.1", "1.3.0", "1.2.8-SNAPSHOT", "1.2.7", "1.2.6");

        DeploymentResource deployment = deployments.deploymentSubResourceByContextRoot(FOO);

        assertEquals("1.3.1", deployment.getVersion().toString());
        assertEquals(6, deployment.getAvailableVersions().size());
        assertEquals("1.3.2", deployment.getAvailableVersions().get(0).getVersion().toString());
        assertEquals("1.3.1", deployment.getAvailableVersions().get(1).getVersion().toString());
        assertEquals("1.3.0", deployment.getAvailableVersions().get(2).getVersion().toString());
        assertEquals("1.2.8-SNAPSHOT", deployment.getAvailableVersions().get(3).getVersion().toString());
        assertEquals("1.2.7", deployment.getAvailableVersions().get(4).getVersion().toString());
        assertEquals("1.2.6", deployment.getAvailableVersions().get(5).getVersion().toString());
    }
}
