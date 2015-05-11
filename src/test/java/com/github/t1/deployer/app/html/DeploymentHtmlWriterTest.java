package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.model.Deployment.*;
import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.app.DeploymentResource;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentHtmlWriterTest extends AbstractHtmlWriterTest<DeploymentResource> {
    private static final DeploymentName NAME = new DeploymentName("foo");
    private static final ContextRoot CONTEXT_ROOT = new ContextRoot("foox");
    private static final CheckSum CHECK_SUM = CheckSum.ofHexString("aabbcc");

    public DeploymentHtmlWriterTest() {
        super(new DeploymentHtmlWriter());
    }

    @InjectMocks
    DeploymentResource resource;

    @Mock
    Repository repository;

    @Before
    public void before() {
        when(repository.availableVersionsFor(CHECK_SUM)).thenReturn(asList( //
                deploymentFor(CONTEXT_ROOT, new Version("1.0")), //
                deploymentFor(CONTEXT_ROOT, new Version("2.0"))));
    }

    private void given(Deployment deployment) {
        resource.deployment(deployment);
    }

    @Test
    public void shouldWriteNewDeploymentForm() throws Exception {
        given(NULL_DEPLOYMENT);

        String entity = write(resource);

        assertEquals(readFile(), entity);
    }

    @Test
    public void shouldWriteExistingDeploymentForm() throws Exception {
        given(new Deployment(NAME, CONTEXT_ROOT, CHECK_SUM).version(new Version("2.0")));

        String entity = write(resource);

        assertEquals(readFile(), entity);
    }
}
