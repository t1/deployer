package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.model.Deployment.*;
import static java.util.Arrays.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.*;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentHtmlWriterTest extends AbstractHtmlWriterTest<Deployment> {
    private static final DeploymentName NAME = new DeploymentName("foo");
    private static final ContextRoot CONTEXT_ROOT = new ContextRoot("foox");
    private static final CheckSum CHECK_SUM = CheckSum.ofHexString("aabbcc");
    private static final CheckSum CHECK_SUM_1_0 = CheckSum.ofHexString("FACE0000723C2C2081E2AA4154545F0D17B46C3E");
    private static final CheckSum CHECK_SUM_2_0 = CheckSum.ofHexString("FACE00006A07A48D123262BD5ADB9E0816E42F97");

    public DeploymentHtmlWriterTest() {
        super(new DeploymentHtmlWriter());
    }

    @Test
    public void shouldWriteNewDeploymentForm() throws Exception {
        String entity = write(NEW_DEPLOYMENT);

        assertEquals(readFile(), entity);
    }

    @Test
    public void shouldWriteExistingDeploymentForm() throws Exception {
        Deployment deployment = new Deployment(NAME, CONTEXT_ROOT, CHECK_SUM, new Version("2.0")) //
                .withAvailableVersions(asList( //
                        new VersionInfo(new Version("1.0"), CHECK_SUM_1_0), //
                        new VersionInfo(new Version("2.0"), CHECK_SUM_2_0) //
                ));

        String entity = write(deployment);

        assertEquals(readFile(), entity);
    }
}
