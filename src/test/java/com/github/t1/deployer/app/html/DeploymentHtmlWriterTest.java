package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.model.Deployment.*;
import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.ws.rs.ext.MessageBodyWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentHtmlWriterTest extends AbstractHtmlWriterTest<Deployment> {
    private static final DeploymentName NAME = new DeploymentName("foo");
    private static final ContextRoot CONTEXT_ROOT = new ContextRoot("foox");
    private static final CheckSum CHECK_SUM = CheckSum.ofHexString("aabbcc");

    public DeploymentHtmlWriterTest() {
        super(writer());
    }

    private static MessageBodyWriter<Deployment> writer() {
        DeploymentHtmlWriter writer = new DeploymentHtmlWriter();
        writer.repository = mock(Repository.class);
        when(writer.repository.availableVersionsFor(CHECK_SUM)).thenReturn(asList( //
                deploymentFor(CONTEXT_ROOT, new Version("1.0")), //
                deploymentFor(CONTEXT_ROOT, new Version("2.0"))));
        return writer;
    }

    @Test
    public void shouldWriteNewDeploymentForm() throws Exception {
        Deployment deployment = NULL_DEPLOYMENT;

        String entity = write(deployment);

        assertEquals(readFile(), entity);
    }

    @Test
    public void shouldWriteExistingDeploymentForm() throws Exception {
        Deployment deployment = new Deployment(NAME, CONTEXT_ROOT, CHECK_SUM);

        String entity = write(deployment);

        assertEquals(readFile(), entity);
    }
}
