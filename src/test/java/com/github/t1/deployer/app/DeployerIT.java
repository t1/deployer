package com.github.t1.deployer.app;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.testtools.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.groups.Tuple;
import org.jboss.arquillian.junit.*;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.StringReader;

import static com.github.t1.log.LogLevel.*;
import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;

@Slf4j
@RunWith(Arquillian.class)
@SuppressWarnings("CdiInjectionPointsInspection")
public class DeployerIT {
    private static final String DEPLOYER_IT_WAR = "deployer-it.war";
    private static final Tuple SELF = tuple(DEPLOYER_IT_WAR, null);

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive createDeployment() {
        return new WebArchiveBuilder(DEPLOYER_IT_WAR)
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
    @Inject LoggerContainer loggerContainer;

    @Before
    public void setup() throws Exception {
        if (first) {
            first = false;

            jbossConfig = new FileMemento(System.getProperty("jboss.server.config.dir") + "/standalone.xml").setup();
            jbossConfig.setOrig(jbossConfig.getOrig().replaceFirst(""
                    + "        <deployment name=\"" + DEPLOYER_IT_WAR + "\" runtime-name=\"" + DEPLOYER_IT_WAR + "\">\n"
                    + "            <content sha1=\"[0-9a-f]{40}\"/>\n"
                    + "        </deployment>\n", ""));
            // restore after JBoss is down
            jbossConfig.restoreOnShutdown().after(100, MILLISECONDS); // hell won't freeze over if this is too fast

            loggerContainer.getHandler("CONSOLE").setLevel(ALL);
            loggerContainer.add(new LoggerConfig("com.github.t1.deployer", DEBUG));

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
    @InSequence(value = 200)
    @Ignore
    public void shouldDeploySecondWebArchive() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"
                + "org.mock-server:\n"
                + "  mockserver-war:\n"
                + "    version: 3.10.4\n"));

        deployer.run(plan);

        // TODO check that jolokia was not redeployed
        assertDeployments(SELF, tuple("jolokia-war", "1.3.2"), tuple("mockserver-war", "3.10.4"));
    }


    @Test
    @InSequence(value = 300)
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
        // TODO pin DEPLOYER_IT_WAR & manage configs
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader("---\n"));

        deployer.run(plan);

        assertDeployments(SELF);
        // TODO assertThat(jbossConfig.read()).isEqualTo(jbossConfig.getOrig());
    }
}
