package com.github.t1.deployer.app;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.testtools.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.groups.Tuple;
import org.jboss.arquillian.junit.*;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.t1.log.LogLevel.*;
import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;

@Slf4j
@RunWith(Arquillian.class)
@SuppressWarnings("CdiInjectionPointsInspection")
public class DeployerIT {
    private static final DeploymentName DEPLOYER_IT = new DeploymentName("deployer-it");
    private static final String DEPLOYER_IT_WAR = DEPLOYER_IT + ".war";

    private static final DeploymentName JOLOKIA_WAR = new DeploymentName("jolokia-war");
    private static final CheckSum JOLOKIA_1_3_2_CHECKSUM =
            CheckSum.fromString("9E29ADD9DF1FA9540654C452DCBF0A2E47CC5330");
    private static final CheckSum JOLOKIA_1_3_3_CHECKSUM =
            CheckSum.fromString("F6E5786754116CC8E1E9261B2A117701747B1259");

    private static final DeploymentName MOCKSERVER_WAR = new DeploymentName("mockserver-war");
    private static final CheckSum MOCKSERVER_3_10_4_CHECKSUM =
            CheckSum.fromString("CD60FEDE66361C2001629B8D6A8427372641EF81");

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
            assertDeployments();
        }
    }

    private void assertDeployments(Tuple... tuples) {
        AtomicInteger i = new AtomicInteger(0);
        container.getAllDeployments().stream().filter(DEPLOYER_IT::matches).forEach(deployment -> {
            Object[] tuple = tuples[i.getAndIncrement()].toArray();
            assertThat(deployment.getName()).isEqualTo(tuple[0]);
            assertThat(deployment.getCheckSum()).isEqualTo(tuple[1]);
        });
    }


    @Test
    @InSequence(value = 100)
    public void shouldDeployWebArchive() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"));

        deployer.run(plan);

        assertDeployments(tuple(JOLOKIA_WAR, JOLOKIA_1_3_2_CHECKSUM));
    }

    @Test
    @InSequence(value = Integer.MAX_VALUE)
    public void shouldUndeployEverything() throws Exception {
        // TODO pin DEPLOYER_IT_WAR & manage configs
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader("---\n"));

        deployer.run(plan);

        assertDeployments();
        // TODO assertThat(jbossConfig.read()).isEqualTo(jbossConfig.getOrig());
    }
}
