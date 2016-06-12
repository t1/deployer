package com.github.t1.deployer.app;

import com.github.t1.deployer.model.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.*;

@RunWith(Arquillian.class)
public class DeployerIT {
    private static final DeploymentName JOLOKIA_ARTIFACT_NAME = new DeploymentName("jolokia-war");
    private static final ContextRoot JOLOKIA_CONTEXT_ROOT = new ContextRoot("jolokia");
    private static final CheckSum JOLOKIA_CHECK_SUM_132
            = CheckSum.fromString("9E29ADD9DF1FA9540654C452DCBF0A2E47CC5330");
    private static final CheckSum JOLOKIA_CHECK_SUM_133
            = CheckSum.fromString("F6E5786754116CC8E1E9261B2A117701747B1259");
    private static final Version JOLOKIA_V132 = new Version("1.3.2");
    private static final Version JOLOKIA_V133 = new Version("1.3.3");
    private static final Deployment JOLOKIA_1_3_2 = JOLOKIA(JOLOKIA_V132, JOLOKIA_CHECK_SUM_132);
    private static final Deployment JOLOKIA_1_3_3 = JOLOKIA(JOLOKIA_V133, JOLOKIA_CHECK_SUM_133);

    private static Deployment JOLOKIA(Version version, CheckSum checkSum) {
        return new Deployment(JOLOKIA_ARTIFACT_NAME, JOLOKIA_CONTEXT_ROOT, checkSum, version);
    }

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                         .addClasses(Deployer.class)
                         .addPackage(Deployment.class.getPackage())
                         .addAsLibraries(Maven
                                 .resolver()
                                 .loadPomFromFile("pom.xml")
                                 .resolve("org.assertj:assertj-core")
                                 .withTransitivity()
                                 .asFile())
                         .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Deployer deployer;

    private void givenConfig(String config) {}

    @Test
    public void shouldDeployWar() throws Exception {
        givenConfig("org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n");

        deployer.run();

        assertThat(deployer.getDeployments()).isNull();//.containsExactly(JOLOKIA_1_3_2);
    }

    // TODO shouldReplaceExistingWarByChecksum
    // TODO shouldNotUndeployOtherWarByDefault
    // TODO shouldUndeployOtherWarWhenConfigured
    // TODO shouldDeployJdbcDriver
    // TODO shouldDeployBundle
    // TODO shouldDeployTemplate
}
