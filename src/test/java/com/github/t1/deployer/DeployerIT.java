package com.github.t1.deployer;

import static com.github.t1.deployer.ArtifactoryMock.*;
import static com.github.t1.deployer.TestData.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.net.URI;
import java.security.Principal;
import java.util.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import lombok.extern.java.Log;

import org.glassfish.hk2.api.*;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Matchers;

@Log
public class DeployerIT {
    private static Container container = mock(Container.class);
    private static Repository repository = mock(Repository.class);
    private static Audit audit = mock(Audit.class);
    private static DeploymentsList deploymentsList = mock(DeploymentsList.class);
    private static Principal principal = new Principal() {
        @Override
        public String getName() {
            return "the-prince";
        }
    };

    @ClassRule
    public static DropwizardClientRule deployer = new DropwizardClientRule( //
            new Deployments(), //
            new LoggingFilter(log, true), //
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(repository).to(Repository.class);
                    bind(container).to(Container.class);
                    bind(audit).to(Audit.class);
                    bind(principal).to(Principal.class);

                    final FactoryInstance<DeploymentHtmlWriter> htmlDeployments =
                            new FactoryInstance<>(new Factory<DeploymentHtmlWriter>() {
                                @Override
                                public DeploymentHtmlWriter provide() {
                                    DeploymentHtmlWriter result = new DeploymentHtmlWriter();
                                    return result;
                                }

                                @Override
                                public void dispose(DeploymentHtmlWriter instance) {}
                            });

                    final UriInfo uriInfo = mock(UriInfo.class);
                    UriBuilder uriBuilder = mock(UriBuilder.class);
                    when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
                    when(uriBuilder.path(Matchers.any(Class.class))).thenReturn(uriBuilder);
                    when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
                    when(uriBuilder.build()).thenReturn(URI.create("http://no.where"));
                    when(uriBuilder.matrixParam(anyString(), Matchers.any(Object[].class))).thenReturn(uriBuilder);
                    bind(new FactoryInstance<>(new Factory<DeploymentResource>() {
                        @Override
                        public DeploymentResource provide() {
                            DeploymentResource result = new DeploymentResource();
                            result.container = container;
                            result.repository = repository;
                            result.audit = audit;
                            result.deploymentsList = deploymentsList;
                            result.htmlDeployments = htmlDeployments;
                            result.uriInfo = uriInfo;
                            return result;
                        }

                        @Override
                        public void dispose(DeploymentResource instance) {}
                    })).to(new TypeLiteral<javax.enterprise.inject.Instance<DeploymentResource>>() {});

                    bind(htmlDeployments).to(
                            new TypeLiteral<javax.enterprise.inject.Instance<DeploymentHtmlWriter>>() {});

                    bind(new FactoryInstance<>(new Factory<DeploymentsListHtmlWriter>() {
                        @Override
                        public DeploymentsListHtmlWriter provide() {
                            DeploymentsListHtmlWriter result = new DeploymentsListHtmlWriter();
                            result.deployments = new ArrayList<>();
                            result.principal = principal;
                            return result;
                        }

                        @Override
                        public void dispose(DeploymentsListHtmlWriter instance) {}
                    })).to(new TypeLiteral<javax.enterprise.inject.Instance<DeploymentsListHtmlWriter>>() {});

                    bind(new FactoryInstance<>(new Factory<NewDeploymentFormHtmlWriter>() {
                        @Override
                        public NewDeploymentFormHtmlWriter provide() {
                            NewDeploymentFormHtmlWriter result = new NewDeploymentFormHtmlWriter();
                            return result;
                        }

                        @Override
                        public void dispose(NewDeploymentFormHtmlWriter instance) {}
                    })).to(new TypeLiteral<javax.enterprise.inject.Instance<NewDeploymentFormHtmlWriter>>() {});

                    bind(deploymentsList).to(DeploymentsList.class);
                }
            });

    @Before
    public void before() {
        reset(container, repository, audit);
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
            log.info("starting: " + description.getMethodName());
        }
    };

    private WebTarget deploymentsWebTarget(ContextRoot contextRoot) {
        return deploymentsWebTarget() //
                .matrixParam("context-root", contextRoot);
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

    private void given(ContextRoot... contextRoots) {
        givenDeployments(repository, contextRoots);
        givenDeployments(container, contextRoots);
    }

    @Test
    public void shouldGetAllDeployments() {
        given(FOO, BAR);

        Response response = deployer() //
                .path("deployments/*") //
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
        givenDeployments(repository, FOO, BAR);
        givenDeployments(container, BAR);

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

        assertStatus(NO_CONTENT, response);
        verify(audit).redeploy(FOO, NEWEST_FOO_VERSION);
        verify(container).redeploy(FOO_WAR, inputStreamFor(FOO, NEWEST_FOO_VERSION));
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

    @Test
    public void shouldPostDeploy() {
        givenDeployments(repository, FOO, BAR);
        givenDeployments(container, BAR);

        Response response = deploymentsWebTarget().request() //
                .post(Entity.form(new Form("action", "deploy") //
                        .param("checkSum", fakeChecksumFor(FOO, CURRENT_FOO_VERSION).toString()) //
                        ));

        assertStatus(OK, response); // redirected
        verify(container).deploy(FOO_WAR, inputStreamFor(FOO, CURRENT_FOO_VERSION));
        verify(audit).deploy(FOO, CURRENT_FOO_VERSION);
    }

    @Test
    public void shouldPostRedeploy() {
        given(FOO, BAR);

        WebTarget uri = deploymentsWebTarget();
        Response response = uri.request() //
                .post(Entity.form(deploymentForm("redeploy", FOO, NEWEST_FOO_VERSION)));

        assertStatus(OK, response); // redirected
        verify(audit).redeploy(FOO, NEWEST_FOO_VERSION);
        verify(container).redeploy(FOO_WAR, inputStreamFor(FOO, NEWEST_FOO_VERSION));
    }

    @Test
    public void shouldPostUndeploy() {
        given(FOO, BAR);

        Response response = deploymentsWebTarget(FOO) //
                .request() //
                .post(Entity.form(deploymentForm("undeploy", FOO, NEWEST_FOO_VERSION)));

        assertStatus(OK, response); // redirected
        verify(audit).undeploy(FOO, CURRENT_FOO_VERSION);
    }

    @Test
    public void shouldGetDeploymentsForm() {
        Response response = deployer() //
                .path("deployments/deployment-form") //
                .request(TEXT_HTML) //
                .get();

        assertStatus(OK, response);
        assertThat(
                response.readEntity(String.class),
                allOf(containsString("Add Deployment"), containsString("<form"),
                        containsString("name=\"action\" value=\"deploy\"")));
    }
}
