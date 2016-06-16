package com.github.t1.deployer.app;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.testtools.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.groups.Tuple;
import org.jboss.arquillian.junit.*;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@RunWith(Arquillian.class)
public class DeployerIT {
    private static final String DEPLOYMENT_NAME = "deployer-it.war";
    private static final Tuple SELF = tuple(DEPLOYMENT_NAME, null);

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive createDeployment() {
        return new WebArchiveBuilder(DEPLOYMENT_NAME)
                .with(Deployer.class.getPackage())
                .with(TestLoggerRule.class, FileMemento.class)
                .library("org.assertj", "assertj-core")
                .print()
                .build();
    }

    @Rule public TestLoggerRule logger = new TestLoggerRule();
    private static FileMemento jbossConfig;
    private static boolean first = true;

    @Inject Deployer deployer;
    @Inject DeploymentContainer container;

    @Before
    public void checkContainerIsClean() throws Exception {
        if (first) {
            first = false;

            jbossConfig = new FileMemento(System.getProperty("jboss.server.config.dir") + "/standalone.xml");

            log.info("deployments: {}", container.getAllDeployments());
            assertDeployments(SELF);
        }
    }

    private void assertDeployments(Tuple... tuples) {
        assertThat(container.getAllDeployments()).extracting(
                deployment -> deployment.getName().getValue(),
                deployment -> (deployment.getVersion() == null) ? null : deployment.getVersion().getVersion()
        ).contains(tuples);
    }

    @AfterClass
    public static void checkAndRestoreJbossConfig() throws Exception {
        if (jbossConfig == null) {
            log.warn("running on client?");
        } else {
            String before = jbossConfig.getOrig();
            String now = jbossConfig.read();
            jbossConfig.restore();
            assertThat(now).isEqualTo(before);
        }
    }


    @Test
    @InSequence(value = 100)
    public void shouldDeployWebArchive() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"));

        deployer.run(plan);

        assertDeployments(SELF, tuple("jolokia-war", null));
    }


    @Test
    @InSequence(value = 150)
    @Ignore
    public void shouldDeployTwoWebArchives() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"
                + "org.mock-server:\n"
                + "  mockserver-war:\n"
                + "    version: 3.10.4\n"));

        deployer.run(plan);

        assertDeployments(SELF, tuple("jolokia-war", "1.3.2"), tuple("mockserver-war", "3.10.4"));
    }


    @Test
    @InSequence(value = 200)
    @Ignore
    public void shouldReplaceExistingWebArchiveByChecksum() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.3\n"));

        deployer.run(plan);

        assertDeployments(SELF, tuple("jolokia-war", "1.3.3"));
    }

    // TODO shouldNotRedeployWebArchiveWithSameNameAndChecksum
    // TODO shouldUndeployWebArchiveWhenStateIsUndeployed
    // TODO shouldRedeployWebArchiveWhenStateIsRedeployed
    // TODO shouldNotUndeployUnspecifiedWebArchiveWhenUnmanaged
    // TODO shouldUndeployUnspecifiedWebArchiveWhenManaged
    // TODO shouldDeployJdbcDriver
    // TODO shouldDeployBundle
    // TODO shouldDeployTemplate

    @Test
    @InSequence(value = Integer.MAX_VALUE)
    public void shouldUndeployEverything() throws Exception {
        // TODO pin DEPLOYMENT_NAME & manage configs
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader("---\n"));

        deployer.run(plan);

        assertDeployments(SELF);
    }
}
