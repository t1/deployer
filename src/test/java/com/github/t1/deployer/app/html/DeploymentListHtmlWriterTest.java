package com.github.t1.deployer.app.html;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.*;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentListHtmlWriterTest extends AbstractHtmlWriterTest<List<Deployment>> {
    public DeploymentListHtmlWriterTest() {
        super(new DeploymentListHtmlWriter());
    }

    @Test
    public void shouldWriteDataSourceList() throws Exception {
        List<Deployment> deployments = asList( //
                new Deployment(new DeploymentName("foo"), new ContextRoot("foox"), CheckSum.ofHexString("aabbcc")), //
                new Deployment(new DeploymentName("bar"), new ContextRoot("barx"), CheckSum.ofHexString("ddeeff")));

        String entity = write(deployments);

        assertEquals(readFile(), entity);
    }
}
