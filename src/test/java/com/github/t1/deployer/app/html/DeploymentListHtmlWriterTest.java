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

    private Deployment deployment(String name, String root, String checksum, String version) {
        return new Deployment(new DeploymentName(name), new ContextRoot(root), CheckSum.ofHexString(checksum),
                new Version(version));
    }

    @Test
    public void shouldWriteDeploymentList() throws Exception {
        List<Deployment> deployments = asList( //
                deployment("foo", "foox", "aabbcc", "2.3.1"), //
                deployment("bar", "barx", "ddeeff", "1.0") //
        );

        String entity = write(deployments);

        assertEquals(readFile(), entity);
    }
}
