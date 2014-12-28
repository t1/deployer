package com.github.t1.deployer;

import static com.github.t1.deployer.TestData.*;
import static javax.ws.rs.core.MediaType.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.net.URI;
import java.nio.file.*;
import java.util.List;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;

public class DeployerITest {
    private static ModelControllerClient cli = mock(ModelControllerClient.class);
    private static Repository repository = mock(Repository.class);

    @ClassRule
    public static DropwizardClientRule deployer = new DropwizardClientRule(new Deployments(), //
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(cli).to(ModelControllerClient.class);
                    bind(repository).to(Repository.class);
                    bind(DeploymentsContainer.class).to(DeploymentsContainer.class);
                }
            });

    private WebTarget deployer() {
        URI baseUri = deployer.baseUri();
        // URI baseUri = URI.create("http://localhost:8080/deployer/");
        return ClientBuilder.newClient().target(baseUri);
    }

    private void assertDeployment(Deployment deployment) {
        assertEquals("foo.war", deployment.getName());
        assertEquals("foo", deployment.getContextRoot());
        assertEquals("1.3.1", deployment.getVersion().toString());
    }

    @Test
    public void shouldGetAllDeployments() {
        givenDeployments(cli, repository, FOO, BAR);

        Response response = deployer() //
                .path("/deployments/*") //
                .request(APPLICATION_JSON_TYPE) //
                .get();

        assertEquals(200, response.getStatus());
        List<Deployment> deployments = response.readEntity(new GenericType<List<Deployment>>() {});
        assertEquals(2, deployments.size());

        Deployment foo = deployments.get(0);
        assertEquals("foo", foo.getContextRoot());
        assertEquals(CURRENT_FOO_VERSION, foo.getVersion());

        Deployment bar = deployments.get(1);
        assertEquals("bar", bar.getContextRoot());
        assertEquals(CURRENT_BAR_VERSION, bar.getVersion());
    }

    @Test
    public void shouldGetDeploymentByContextRoot() {
        givenDeployments(cli, repository, FOO, BAR);

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
        givenDeployments(cli, repository, FOO, BAR);

        Response response = deployer() //
                .path("/deployments") //
                .matrixParam("context-root", "deployer") //
                .path("available-versions") //
                .request(APPLICATION_JSON_TYPE) //
                .get();

        assertEquals(200, response.getStatus());
        List<Version> versions = response.readEntity(new GenericType<List<Version>>() {});
        assertEquals(FOO_VERSIONS.toString(), versions.toString());
    }

    @Test
    @Ignore("test doesn't work, yet")
    public void shouldDeploy() throws Exception {
        givenDeployments(cli, repository, FOO, BAR);
        when(cli.execute(any(Operation.class), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(success("\"x\"")));

        byte[] bytes = Files.readAllBytes(Paths.get("foo.war"));
        Entity<?> deployment = Entity.entity(bytes, APPLICATION_ATOM_XML_TYPE);
        Response response = deployer() //
                .path("/deployments") //
                .matrixParam("context-root", "foo") //
                .request(APPLICATION_JSON_TYPE) //
                .put(deployment);

        assertEquals(201, response.getStatus());
        System.out.println("--> " + response.readEntity(String.class));
        System.out.println("++> " + response.getLocation());
    }

    @Test
    @Ignore("test doesn't work, yet")
    public void shouldRedeploy() throws Exception {
        givenDeployments(cli, repository, FOO, BAR);

        byte[] bytes = Files.readAllBytes(Paths.get("foo.war"));
        Entity<?> deployment = Entity.entity(bytes, APPLICATION_ATOM_XML_TYPE);
        Response response = deployer() //
                .path("/deployments") //
                .matrixParam("context-root", "foo") //
                .request(APPLICATION_JSON_TYPE) //
                .put(deployment);

        assertEquals(201, response.getStatus());
        System.out.println("--> " + response.readEntity(String.class));
        System.out.println("++> " + response.getLocation());
    }

    @Test
    @Ignore("test doesn't work, yet")
    public void shouldUndeploy() {
        givenDeployments(cli, repository, FOO, BAR);

        Response response = deployer() //
                .path("/deployments") //
                .matrixParam("context-root", "foo") //
                .request(APPLICATION_JSON_TYPE) //
                .delete();

        assertEquals(204, response.getStatus());
    }
}
