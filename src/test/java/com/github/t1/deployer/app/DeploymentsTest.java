package com.github.t1.deployer.app;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;

import javax.xml.bind.JAXB;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import lombok.Value;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentsTest {
    private static final ObjectMapper JSON = new ObjectMapper().setSerializationInclusion(NON_NULL);

    @Value
    public class OngoingDeploymentStub {
        Deployment deployment;

        public OngoingDeploymentStub withReleases(String... versionStrings) {
            List<Release> releases = new ArrayList<>();
            for (String versionString : versionStrings) {
                Version version = new Version(versionString);
                CheckSum checkSum = fakeChecksumFor(deployment.getContextRoot(), version);
                releases.add(new Release(version, checkSum));
            }
            when(repository.releasesFor(deployment.getCheckSum())).thenReturn(releases);
            return this;
        }
    }

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
    }

    private OngoingDeploymentStub givenDeployment(ContextRoot contextRoot) {
        Deployment deployment = deploymentFor(contextRoot);
        installedDeployments.add(deployment);
        when(repository.getByChecksum(fakeChecksumFor(contextRoot))).thenReturn(deploymentFor(contextRoot));
        when(container.getDeploymentFor(contextRoot)).thenReturn(deployment);
        return new OngoingDeploymentStub(deploymentFor(contextRoot, fakeVersionFor(contextRoot)));
    }

    private void assertReleases(ContextRoot contextRoot, List<Release> actual, String... versions) {
        assertEquals(versions.length, actual.size());
        int i = 0;
        for (Release entry : actual) {
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
    public void shouldGetDeploymentByContextRoot() {
        givenDeployment(FOO).withReleases("1.3.1");

        Deployment deployment = deployments.getByContextRoot(FOO);

        assertEquals("1.3.1", deployment.getVersion().toString());
        assertReleases(deployment.getContextRoot(), deployment.getReleases(), "1.3.1");
    }

    @Test
    public void shouldGetDeploymentVersions() {
        String[] versions = { "1.2.6", "1.2.7", "1.2.8-SNAPSHOT", "1.3.0", "1.3.1", "1.3.2" };
        givenDeployment(FOO).withReleases(versions);

        Deployment deployment = deployments.getByContextRoot(FOO);

        assertReleases(FOO, deployment.getReleases(), versions);
    }

    private static final String DEPLOYMENT_JSON = "{" //
            + "\"name\":\"foo.war\"," //
            + "\"contextRoot\":\"foo\"," //
            + "\"checkSum\":\"FACE000094D353F082E6939015AF81D263BA0F8F\"," //
            + "\"version\":\"1.3.1\"" //
            + "}";

    private static final String RESOURCE_JSON = "{" //
            + "\"name\":\"foo.war\"," //
            + "\"contextRoot\":\"foo\"," //
            + "\"checkSum\":\"FACE000094D353F082E6939015AF81D263BA0F8F\"," //
            + "\"version\":\"1.3.1\"," //
            + "\"releases\":[" //
            + "{\"version\":\"1.0\",\"checkSum\":\"FACE0000949FD646CD3A0D9AF75635813FAE3225\"}," //
            + "{\"version\":\"1.1\",\"checkSum\":\"FACE0000BDA60DDA3EBCF32ABB974013CCDDC2F7\"}," //
            + "{\"version\":\"2.0\",\"checkSum\":\"FACE0000A962CF1E5BE6E12ED6BBD283620DC64B\"}" //
            + "]" //
            + "}";

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
    public void shouldSerializeDeploymentAsJson() throws IOException {
        Deployment deployment = deploymentFor(FOO);

        StringWriter stringWriter = new StringWriter();
        JSON.writeValue(stringWriter, deployment);

        assertEquals(DEPLOYMENT_JSON, stringWriter.toString());
    }

    @Test
    public void shouldDeserializeDeploymentFromJson() throws IOException {
        Deployment deployment = deploymentFor(FOO);

        Deployment actual = JSON.readValue(DEPLOYMENT_JSON, Deployment.class);

        assertEquals(deployment, actual);
    }

    @Test
    public void shouldSerializeResourceAsJson() throws IOException {
        givenDeployment(FOO).withReleases("1.0", "1.1", "2.0");
        Deployment deployment = deployments.getByContextRoot(FOO);

        StringWriter stringWriter = new StringWriter();
        JSON.writeValue(stringWriter, deployment);

        assertEquals(RESOURCE_JSON, stringWriter.toString());
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
    public void shouldMarshalResourceAsXml() {
        givenDeployment(FOO).withReleases("1.0", "1.1", "2.0");
        Deployment deployment = deployments.getByContextRoot(FOO);

        StringWriter writer = new StringWriter();
        JAXB.marshal(deployment, writer);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" //
                + "<deployment>\n" //
                + "    <name>foo.war</name>\n" //
                + "    <contextRoot>foo</contextRoot>\n" //
                + "    <checkSum>+s4AAJTTU/CC5pOQFa+B0mO6D48=</checkSum>\n" //
                + "    <version>1.3.1</version>\n" //
                + "    <releases>\n" //
                + "        <release checkSum=\"+s4AAJSf1kbNOg2a91Y1gT+uMiU=\">1.0</release>\n" //
                + "        <release checkSum=\"+s4AAL2mDdo+vPMqu5dAE8zdwvc=\">1.1</release>\n" //
                + "        <release checkSum=\"+s4AAKlizx5b5uEu1rvSg2INxks=\">2.0</release>\n" //
                + "    </releases>\n" //
                + "</deployment>\n" //
                , writer.toString());
    }
}
