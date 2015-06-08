package com.github.t1.deployer.app;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;

import javax.xml.bind.JAXB;

import org.junit.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.t1.deployer.TestData.OngoingDeploymentStub;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;

public class DeploymentResourceTest {
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
            + "\"availableVersions\":[" //
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

    private static final ObjectMapper JSON = new ObjectMapper().setSerializationInclusion(NON_NULL);

    Deployment deployment = deploymentFor(FOO);
    DeploymentResource deploymentResource = new DeploymentResource().deployment(deployment);

    @Before
    public void setup() {
        deploymentResource.repository = mock(Repository.class);
        new OngoingDeploymentStub(deploymentResource.repository, deployment).availableVersions("1.0", "1.1", "2.0");
    }

    @Test
    public void shouldSerializeDeploymentAsJson() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JSON.writeValue(stringWriter, deployment);

        assertEquals(DEPLOYMENT_JSON, stringWriter.toString());
    }

    @Test
    public void shouldDeserializeDeploymentFromJson() throws IOException {
        Deployment actual = JSON.readValue(DEPLOYMENT_JSON, Deployment.class);

        assertEquals(deployment, actual);
    }

    @Test
    public void shouldSerializeResourceAsJson() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JSON.writeValue(stringWriter, deploymentResource);

        assertEquals(RESOURCE_JSON, stringWriter.toString());
    }

    @Test
    public void shouldMarshalDeploymentAsXml() {
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
        StringWriter writer = new StringWriter();

        JAXB.marshal(deploymentResource, writer);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" //
                + "<deployment>\n" //
                + "    <name>foo.war</name>\n" //
                + "    <contextRoot>foo</contextRoot>\n" //
                + "    <checkSum>+s4AAJTTU/CC5pOQFa+B0mO6D48=</checkSum>\n" //
                + "    <version>1.3.1</version>\n" //
                + "    <availableVersions>\n" //
                + "        <availableVersion checkSum=\"+s4AAJSf1kbNOg2a91Y1gT+uMiU=\">1.0</availableVersion>\n" //
                + "        <availableVersion checkSum=\"+s4AAL2mDdo+vPMqu5dAE8zdwvc=\">1.1</availableVersion>\n" //
                + "        <availableVersion checkSum=\"+s4AAKlizx5b5uEu1rvSg2INxks=\">2.0</availableVersion>\n" //
                + "    </availableVersions>\n" //
                + "</deployment>\n" //
        , writer.toString());
    }
}
