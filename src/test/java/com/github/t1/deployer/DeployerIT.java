package com.github.t1.deployer;

import static com.github.t1.deployer.ArtifactoryMock.*;
import static com.github.t1.deployer.TestData.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.*;

public class DeployerIT {
    private static Container container = mock(Container.class);
    private static Repository repository = mock(Repository.class);
    private static Audit audit = mock(Audit.class);
    private static Principal principal = new Principal() {
        @Override
        public String getName() {
            return "the-prince";
        }
    };

    @ClassRule
    public static DropwizardClientRule deployer = new DropwizardClientRule(new Deployments(), //
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(repository).to(Repository.class);
                    bind(container).to(Container.class);
                    bind(audit).to(Audit.class);
                    bind(principal).to(Principal.class);

                    System.setProperty("jboss.server.config.dir", "target");
                    bind(DeploymentsList.class).to(DeploymentsList.class);
                }
            });

    @Before
    public void before() {
        reset(container, repository);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(audit);
    }

    private WebTarget deploymentsWebTarget(ContextRoot contextRoot) {
        return deployer() //
                .path("/deployments") //
                .matrixParam("context-root", contextRoot);
    }

    private WebTarget deployer() {
        URI baseUri = deployer.baseUri();
        // URI baseUri = URI.create("http://localhost:8080/deployer/");
        return ClientBuilder.newClient().target(baseUri);
    }

    private void given(ContextRoot... contextRoots) {
        givenDeployments(repository, contextRoots);
        givenDeployments(container, contextRoots);
    }

    @Test
    public void shouldGetAllDeployments() {
        given(FOO, BAR);

        Response response = deployer() //
                .path("/deployments/*") //
                .request(APPLICATION_JSON_TYPE) //
                .get();

        assertStatus(OK, response);
        List<Deployment> deployments = response.readEntity(new GenericType<List<Deployment>>() {});
        assertEquals(2, deployments.size());

        Deployment foo = deployments.get(0);
        assertEquals(FOO, foo.getContextRoot());
        assertEquals(CURRENT_FOO_VERSION, foo.getVersion());

        Deployment bar = deployments.get(1);
        assertEquals(BAR, bar.getContextRoot());
        assertEquals(CURRENT_BAR_VERSION, bar.getVersion());
    }

    @Test
    public void shouldGetDeploymentByContextRoot() {
        given(FOO, BAR);

        Response response = deploymentsWebTarget(FOO) //
                .request(APPLICATION_JSON_TYPE) //
                .get();

        assertStatus(OK, response);
        Deployment deployment = response.readEntity(Deployment.class);
        assertDeployment(FOO, deployment);
    }

    @Test
    @Ignore("sub resources after matrix params don't seem to work in Dropwizard")
    public void shouldGetAvailableVersion() {
        given(FOO, BAR);

        Response response = deploymentsWebTarget(FOO) //
                .path("available-versions") //
                .request(APPLICATION_JSON_TYPE) //
                .get();

        assertStatus(OK, response);
        List<Version> versions = response.readEntity(new GenericType<List<Version>>() {});
        assertEquals(FOO_VERSIONS.toString(), versions.toString());
    }

    @Test
    public void shouldDeploy() {
        given(FOO, BAR);

        WebTarget uri = deploymentsWebTarget(FOO);
        Response response = uri //
                .request(APPLICATION_JSON_TYPE) //
                .put(Entity.json(deploymentJson(FOO, CURRENT_FOO_VERSION)));

        assertStatus(CREATED, response);
        assertEquals(uri.getUri(), response.getLocation());
        verify(container).deploy(FOO_WAR, inputStreamFor(FOO, CURRENT_FOO_VERSION));
        verify(audit).deploy(FOO, CURRENT_FOO_VERSION);
    }

    @Test
    public void shouldUpgrade() {
        given(FOO, BAR);

        WebTarget uri = deploymentsWebTarget(FOO);
        Response response = uri //
                .request(APPLICATION_JSON_TYPE) //
                .put(Entity.json(deploymentJson(FOO, NEWEST_FOO_VERSION)));

        assertStatus(CREATED, response);
        assertEquals(uri.getUri(), response.getLocation());
        verify(audit).deploy(FOO, NEWEST_FOO_VERSION);
        verify(container).deploy(FOO_WAR, inputStreamFor(FOO, NEWEST_FOO_VERSION));
    }

    @Test
    public void shouldUndeploy() {
        given(FOO, BAR);

        Response response = deploymentsWebTarget(FOO) //
                .request(APPLICATION_JSON_TYPE) //
                .delete();

        assertStatus(NO_CONTENT, response);
        verify(audit).undeploy(FOO, CURRENT_FOO_VERSION);
    }
}
