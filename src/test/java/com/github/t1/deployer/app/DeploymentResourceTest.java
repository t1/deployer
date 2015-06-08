package com.github.t1.deployer.app;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;

import org.junit.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;

public class DeploymentResourceTest {
    private static final ObjectMapper JSON = new ObjectMapper().setSerializationInclusion(NON_NULL);

    CheckSum checkSum = fakeChecksumFor(FOO);
    Deployment deployment = new Deployment(FOO_WAR, FOO, checkSum).version(new Version("2.0"));
    DeploymentResource deploymentResource = new DeploymentResource().deployment(deployment);

    @Before
    public void setup() {
        deploymentResource.repository = mock(Repository.class);
        Map<Version, CheckSum> versions = new LinkedHashMap<>();
        versions.put(new Version("1.0"), checkSum);
        versions.put(new Version("1.1"), checkSum);
        versions.put(new Version("2.0"), checkSum);
        when(deploymentResource.repository.availableVersionsFor(checkSum)).thenReturn(versions);
    }

    @Test
    public void shouldSerializeDeploymentAsJson() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JSON.writeValue(stringWriter, deployment);

        assertEquals(
                "{\"name\":\"foo.war\",\"contextRoot\":\"foo\",\"checkSum\":\"FACE000094D353F082E6939015AF81D263BA0F8F\",\"version\":\"2.0\"}",
                stringWriter.toString());
    }

    @Test
    public void shouldSerializeResourceAsJson() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JSON.writeValue(stringWriter, deploymentResource);

        assertEquals("{" //
                + "\"name\":\"foo.war\"," //
                + "\"contextRoot\":\"foo\"," //
                + "\"checkSum\":\"FACE000094D353F082E6939015AF81D263BA0F8F\"," //
                + "\"version\":\"2.0\"," //
                + "\"availableVersions\":{" //
                + "\"1.0\":\"FACE000094D353F082E6939015AF81D263BA0F8F\"," //
                + "\"1.1\":\"FACE000094D353F082E6939015AF81D263BA0F8F\"," //
                + "\"2.0\":\"FACE000094D353F082E6939015AF81D263BA0F8F\"" //
                + "}" //
                + "}", stringWriter.toString());
    }
}
