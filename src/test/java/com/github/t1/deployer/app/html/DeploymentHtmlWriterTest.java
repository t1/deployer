package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.model.Deployment.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.TestData.OngoingDeploymentStub;
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

    private void given(Deployment deployment) {
        resource.deployment(deployment);
        new OngoingDeploymentStub(repository, deployment).availableVersions("1.0", "2.0");
    }

    @Test
    public void shouldWriteNewDeploymentForm() throws Exception {
        given(NULL_DEPLOYMENT);

        String entity = write(resource);

        assertEquals(readFile(), entity);
    }

    @Test
    public void shouldWriteExistingDeploymentForm() throws Exception {
        given(new Deployment(NAME, CONTEXT_ROOT, CHECK_SUM, new Version("2.0")));

        String entity = write(resource);

        assertEquals(readFile(), entity);
    }
}
