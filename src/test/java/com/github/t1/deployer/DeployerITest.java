package com.github.t1.deployer;

import static com.github.t1.deployer.DeploymentsContainer.*;
import static com.github.t1.deployer.TestData.*;
import static javax.ws.rs.core.MediaType.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import lombok.SneakyThrows;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;

public class DeployerITest {
    private static ModelControllerClient cli = mock(ModelControllerClient.class);
    private static VersionsGateway versionsGateway = mock(VersionsGateway.class);

    @ClassRule
    public static DropwizardClientRule deployer = new DropwizardClientRule(new Deployments(), //
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(cli).to(ModelControllerClient.class);
                    bind(versionsGateway).to(VersionsGateway.class);
                    bind(DeploymentsContainer.class).to(DeploymentsContainer.class);
                }
            });

    private WebTarget deployer() {
        URI baseUri = deployer.baseUri();
        // URI baseUri = URI.create("http://localhost:8080/deployer/");
        return ClientBuilder.newClient().target(baseUri);
    }

    @SneakyThrows(IOException.class)
    private void givenCliDeployments() {
        ModelNode fooAndBar =
                ModelNode.fromString( //
                        "{\n" + "    \"outcome\" => \"success\",\n" + "    \"result\" => [\n" + "        {\n"
                                + "            \"address\" => [(\"deployment\" => \"foo.war\")],\n"
                                + "            \"outcome\" => \"success\",\n" + "            \"result\" => {\n"
                                + "                \"content\" => [{\"hash\" => bytes {\n"
                                + byteArray(FOO_CHECKSUM) //
                                + "                }}],\n"
                                + "                \"enabled\" => true,\n"
                                + "                \"name\" => \"foo.war\",\n"
                                + "                \"persistent\" => true,\n"
                                + "                \"runtime-name\" => \"foo.war\",\n"
                                + "                \"subdeployment\" => undefined,\n"
                                + "                \"subsystem\" => {\"undertow\" => {\n"
                                + "                    \"context-root\" => \"/foo\",\n"
                                + "                    \"server\" => \"default-server\",\n"
                                + "                    \"virtual-host\" => \"default-host\",\n"
                                + "                    \"servlet\" => {\"javax.ws.rs.core.Application\" => {\n"
                                + "                        \"servlet-class\" => \"org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher\",\n"
                                + "                        \"servlet-name\" => \"javax.ws.rs.core.Application\"\n"
                                + "                    }}\n"
                                + "                }}\n"
                                + "            }\n"
                                + "        },\n"
                                + "        {\n"
                                + "            \"address\" => [(\"deployment\" => \"bar.war\")],\n"
                                + "            \"outcome\" => \"success\",\n"
                                + "            \"result\" => {\n"
                                + "                \"content\" => [{\"hash\" => bytes {\n"
                                + byteArray(BAR_CHECKSUM) //
                                + "                }}],\n"
                                + "                \"enabled\" => true,\n"
                                + "                \"name\" => \"bar.war\",\n"
                                + "                \"persistent\" => true,\n"
                                + "                \"runtime-name\" => \"bar.war\",\n"
                                + "                \"subdeployment\" => undefined,\n"
                                + "                \"subsystem\" => {\"undertow\" => {\n"
                                + "                    \"context-root\" => \"/bar\",\n"
                                + "                    \"server\" => \"default-server\",\n"
                                + "                    \"virtual-host\" => \"default-host\",\n"
                                + "                    \"servlet\" => {\"javax.ws.rs.core.Application\" => {\n"
                                + "                        \"servlet-class\" => \"org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher\",\n"
                                + "                        \"servlet-name\" => \"javax.ws.rs.core.Application\"\n"
                                + "                    }}\n" //
                                + "                }}\n" //
                                + "            }\n" //
                                + "        }\n" //
                                + "    ]\n" //
                                + "}\n" //
                        );
        when(cli.execute(eq(READ_DEPLOYMENTS), any(OperationMessageHandler.class))).thenReturn(fooAndBar);
        when(versionsGateway.searchByChecksum(FOO_CHECKSUM)).thenReturn(CURRENT_FOO_VERSION);
        when(versionsGateway.searchByChecksum(BAR_CHECKSUM)).thenReturn(CURRENT_BAR_VERSION);
        when(versionsGateway.searchVersions("foo-group", "foo-artifact")).thenReturn(FOO_VERSIONS);
    }

    private void assertDeployment(Deployment deployment) {
        assertEquals("foo.war", deployment.getName());
        assertEquals("/foo", deployment.getContextRoot());
        assertEquals("1.3.1", deployment.getVersion().toString());
    }

    @Test
    public void shouldGetAllDeployments() {
        givenCliDeployments();

        Response response = deployer() //
                .path("/deployments/*") //
                .request(APPLICATION_JSON_TYPE) //
                .get();

        assertEquals(200, response.getStatus());
        List<Deployment> deployments = response.readEntity(new GenericType<List<Deployment>>() {});
        assertEquals(2, deployments.size());

        Deployment foo = deployments.get(0);
        assertEquals("/foo", foo.getContextRoot());
        assertEquals(CURRENT_FOO_VERSION, foo.getVersion());

        Deployment bar = deployments.get(1);
        assertEquals("/bar", bar.getContextRoot());
        assertEquals(CURRENT_BAR_VERSION, bar.getVersion());
    }

    @Test
    public void shouldGetDeploymentByContextRoot() {
        givenCliDeployments();

        Response response = deployer() //
                .path("/deployments") //
                .matrixParam("context-root", "foo") //
                .request(APPLICATION_JSON_TYPE) //
                .get();

        assertEquals(200, response.getStatus());
        Deployment deployment = response.readEntity(Deployment.class);
        assertDeployment(deployment);
    }

    @Test
    @Ignore("sub resources after matrix params don't seem to work in Dropwizard")
    public void shouldGetDeploymentAvailableVersionByContextRootMatrix() {
        givenCliDeployments();

        Response response = deployer() //
                .path("/deployments") //
                .matrixParam("context-root", "deployer") //
                .path("/available-versions") //
                .request(APPLICATION_JSON_TYPE) //
                .get();

        assertEquals(200, response.getStatus());
        List<Version> versions = response.readEntity(new GenericType<List<Version>>() {});
        assertEquals(FOO_VERSIONS.toString(), versions.toString());
    }
}
