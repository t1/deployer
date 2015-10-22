package com.github.t1.deployer.app;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.model.Deployment.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Matchers;

import com.github.t1.deployer.app.file.DeploymentListFile;
import com.github.t1.deployer.app.html.DeploymentHtmlWriter;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.deployer.tools.InterceptorMock;
import com.github.t1.rest.RestResource;

import io.dropwizard.testing.junit.DropwizardClientRule;
import lombok.extern.java.Log;

@Log
public class DeploymentsIT {
    private static DeploymentContainer container = mock(DeploymentContainer.class);
    private static Repository repository = mock(Repository.class);
    private static Audit audit = mock(Audit.class);
    private static DeploymentListFile deploymentListFile = mock(DeploymentListFile.class);
    private static Principal principal = mock(Principal.class);

    private static DeploymentContainer interceptedContainer = InterceptorMock.intercept(container).with(interceptor());

    private static DeploymentOperationInterceptor interceptor() {
        DeploymentOperationInterceptor interceptor = new DeploymentOperationInterceptor();
        interceptor.audit = audit;
        interceptor.deploymentsList = deploymentListFile;
        return interceptor;
    }

    @ClassRule
    public static DropwizardClientRule deployer = new DropwizardClientRule( //
            new Deployments(), //
            new LoggingFilter(log, true), //
            new DeploymentHtmlWriter(), //
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(repository).to(Repository.class);
                    bind(interceptedContainer).to(DeploymentContainer.class);
                    bind(audit).to(Audit.class);
                    bind(principal).to(Principal.class);

                    final UriInfo uriInfo = mock(UriInfo.class);
                    bindUriBuilder(uriInfo);

                    bind(deploymentListFile).to(DeploymentListFile.class);
                }

                private void bindUriBuilder(final UriInfo uriInfo) {
                    UriBuilder uriBuilder = mock(UriBuilder.class);
                    when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
                    when(uriBuilder.path(Matchers.any(Class.class))).thenReturn(uriBuilder);
                    when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
                    when(uriBuilder.build()).thenReturn(URI.create("http://no.where"));
                }
            });

    @Before
    public void before() {
        reset(container, repository, audit, principal);
        when(principal.getName()).thenReturn("jbossadmin");
    }

    @After
    public void after() {
        verifyNoMoreInteractions(audit);
    }

    @Rule
    public TestWatcher logRule = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            log.info("starting: " + description.getMethodName());
        }

        @Override
        protected void finished(Description description) {
            log.info("finishing: " + description.getMethodName());
        }
    };

    private WebTarget deploymentsWebTarget(ContextRoot contextRoot) {
        return deploymentsWebTarget().path(contextRoot.getValue());
    }

    private WebTarget deploymentsWebTarget() {
        return deployer() //
                .register(new LoggingFilter(log, false)) //
                .path("deployments");
    }

    private WebTarget deployer() {
        URI baseUri = deployer.baseUri();
        // URI baseUri = URI.create("http://localhost:8080/deployer/");
        return ClientBuilder.newClient().target(baseUri);
    }

    private RestResource deploymentsRestResource(ContextRoot contextRoot) {
        return deployerRestResource(contextRoot.getValue());
    }

    private RestResource deployerRestResource(String path) {
        String baseUri = deployer.baseUri() + "/deployments";
        // URI baseUri = URI.create("http://localhost:8080/deployer/");
        return new RestResource(baseUri + "/" + path);
    }

    private void given(ContextRoot... contextRoots) {
        givenDeployments(repository, contextRoots);
        givenDeployments(container, contextRoots);
    }

    private void assertRedeployLatestFoo(Response response) {
        assertStatus(NO_CONTENT, response);
        verify(audit).allow("redeploy", FOO_WAR);
        verify(container).redeploy(FOO_WAR, inputStreamFor(FOO, NEWEST_FOO_VERSION));
    }

    @Test
    public void shouldGetAllDeployments() {
        given(FOO, BAR);

        Response response = deployer() //
                .path("deployments") //
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

        Deployment deployment = deploymentsRestResource(FOO).GET(Deployment.class);

        assertDeployment(FOO, deployment);
    }

    @Test
    public void shouldGetReleases() {
        given(FOO, BAR);

        Response response = deploymentsWebTarget(FOO) //
                .path("releases") //
                .request(APPLICATION_JSON_TYPE) //
                .get();

        assertStatus(OK, response);
        List<Release> releases = response.readEntity(new GenericType<List<Release>>() {});
        assertEquals(releases(FOO).toString(), releases.toString());
    }

    @Test
    public void shouldGetVersion() {
        given(FOO, BAR);

        Version version = deploymentsWebTarget(FOO) //
                .path("version") //
                .request(APPLICATION_JSON_TYPE) //
                .get(Version.class);

        assertEquals(CURRENT_FOO_VERSION, version);
    }

    @Test
    public void shouldDeploy() {
        givenDeployments(repository, FOO, BAR);
        givenDeployments(container, BAR);

        WebTarget uri = deploymentsWebTarget(FOO);
        Response response = uri //
                .request(APPLICATION_JSON_TYPE) //
                .put(Entity.json(deploymentJson(FOO, CURRENT_FOO_VERSION)));

        assertStatus(CREATED, response);
        assertEquals(uri.getUri(), response.getLocation());
        verify(container).deploy(FOO_WAR, inputStreamFor(FOO, CURRENT_FOO_VERSION));
        verify(audit).allow("deploy", FOO_WAR);
    }

    @Test
    public void shouldUpgrade() {
        given(FOO, BAR);

        WebTarget uri = deploymentsWebTarget(FOO);
        Response response = uri //
                .request(APPLICATION_JSON_TYPE) //
                .put(Entity.json(deploymentJson(FOO, NEWEST_FOO_VERSION)));

        assertRedeployLatestFoo(response);
    }

    @Test
    public void shouldPutVersion() {
        given(FOO, BAR);

        Response response = deploymentsWebTarget(FOO) //
                .path("version") //
                .request(APPLICATION_JSON_TYPE) //
                .put(Entity.json(NEWEST_FOO_VERSION));

        assertRedeployLatestFoo(response);
    }

    @Test
    public void shouldUndeploy() {
        given(FOO, BAR);

        Response response = deploymentsWebTarget(FOO) //
                .request(APPLICATION_JSON_TYPE) //
                .delete();

        assertStatus(NO_CONTENT, response);
        verify(audit).allow("undeploy", FOO_WAR);
    }

    @Test
    public void shouldPostDeploy() {
        givenDeployments(repository, FOO, BAR);
        givenDeployments(container, BAR);

        Response response = deploymentsWebTarget() //
                .request(APPLICATION_JSON_TYPE) //
                .post(Entity.form(new Form("action", "deploy") //
                        .param("checksum", fakeChecksumFor(FOO, CURRENT_FOO_VERSION).toString()) //
        ));

        assertStatus(OK, response); // redirected
        verify(container).deploy(FOO_WAR, inputStreamFor(FOO, CURRENT_FOO_VERSION));
        verify(audit).allow("deploy", FOO_WAR);
    }

    @Test
    public void shouldPostRedeploy() {
        given(FOO, BAR);

        Response response = deploymentsWebTarget(FOO) //
                .request(APPLICATION_JSON_TYPE) //
                .post(Entity.form(new Form("action", "redeploy") //
                        .param("checksum", fakeChecksumFor(FOO, NEWEST_FOO_VERSION).toString()) //
                        .param("contextRoot", FOO.getValue())));

        assertStatus(OK, response); // redirected
        verify(audit).allow("redeploy", FOO_WAR);
        verify(container).redeploy(FOO_WAR, inputStreamFor(FOO, NEWEST_FOO_VERSION));
    }

    @Test
    public void shouldPostUndeploy() {
        given(FOO, BAR);

        // RestResource resource = deploymentsRestResource(FOO).post();
        Response response = deploymentsWebTarget(FOO) //
                .request(APPLICATION_JSON_TYPE) //
                .post(Entity.form(new Form("action", "undeploy") //
                        .param("checksum", fakeChecksumFor(FOO, CURRENT_FOO_VERSION).toString()) //
                        .param("contextRoot", FOO.getValue())));

        assertStatus(OK, response); // redirected
        verify(audit).allow("undeploy", FOO_WAR);
    }

    @Test
    public void shouldGetDeploymentsForm() {
        Response response = deployer() //
                .path("deployments").path(NEW_DEPLOYMENT_PATH) //
                .request(TEXT_HTML) //
                .get();

        assertStatus(OK, response);
        assertThat(response.readEntity(String.class)) //
                .contains("Add Deployment") //
                .contains("<form") //
                .contains("name=\"action\" value=\"deploy\"");
    }
}
