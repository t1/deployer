package com.github.t1.deployer.app;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.ArtifactoryMockLauncher;
import com.github.t1.testtools.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.assertj.core.groups.Tuple;
import org.jboss.arquillian.junit.*;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;

@Slf4j
@RunWith(Arquillian.class)
@SuppressWarnings("CdiInjectionPointsInspection")
public class DeployerIT {
    private static final DeploymentName DEPLOYER_IT = new DeploymentName("deployer-it");
    private static final String DEPLOYER_IT_WAR = DEPLOYER_IT + ".war";

    private static Condition<Deployment> deployment(String name) { return deployment(new DeploymentName(name)); }

    private static Condition<Deployment> deployment(DeploymentName name) {
        return new Condition<>(name::matches, "deployment with name '" + name + "'");
    }

    private static Condition<Deployment> checksum(String checksum) { return checksum(CheckSum.fromString(checksum)); }

    private static Condition<Deployment> checksum(CheckSum checksum) {
        return new Condition<>(checksum::matches, "deployment with checksum '" + checksum + "'");
    }

    private static Condition<Deployment> jolokia_1_3_2() {
        return allOf(deployment("jolokia-war"), checksum("9E29ADD9DF1FA9540654C452DCBF0A2E47CC5330"));
    }

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive createDeployment() {
        return new WebArchiveBuilder(DEPLOYER_IT_WAR)
                .with(Deployer.class.getPackage())
                .with(TestLoggerRule.class, FileMemento.class)
                .library("org.assertj", "assertj-core")
                .print()
                .build();
    }

    static {
        if (runningOnClient())
            try {
                new ArtifactoryMockLauncher().noConsole().run("server");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    private static boolean runningOnClient() { return System.getProperty("jboss.server.config.dir") == null; }


    @Rule public TestLoggerRule logger = new TestLoggerRule();
    private static FileMemento jbossConfig;
    private static boolean first = true;

    @Inject Deployer deployer;
    @Inject DeploymentContainer container;
    @Inject LoggerContainer loggers;

    @Before
    public void setup() throws Exception {
        if (first && !runningOnClient()) {
            first = false;

            jbossConfig = new FileMemento(System.getProperty("jboss.server.config.dir") + "/standalone.xml").setup();
            jbossConfig.setOrig(jbossConfig.getOrig().replaceFirst(""
                    + "        <deployment name=\"" + DEPLOYER_IT_WAR + "\" runtime-name=\"" + DEPLOYER_IT_WAR + "\">\n"
                    + "            <content sha1=\"[0-9a-f]{40}\"/>\n"
                    + "        </deployment>\n", ""));
            // restore after JBoss is down
            jbossConfig.restoreOnShutdown().after(100, MILLISECONDS); // hell won't freeze over if this is too fast

            loggers.handler(console, "CONSOLE").correctLevel(ALL);
            loggers.buildLogger().category("com.github.t1.deployer").level(DEBUG).build().add();

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
        String plan = ""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n";

        deployer.run(plan);

        assertThat(container.getAllDeployments())
                .hasSize(2)
                .haveExactly(1, jolokia_1_3_2())
                .haveExactly(1, deployment(DEPLOYER_IT_WAR));
    }

    @Test
    @InSequence(value = 200)
    public void shouldUndeployWebArchive() throws Exception {
        String plan = ""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    state: undeployed\n";

        deployer.run(plan);

        assertThat(container.getAllDeployments())
                .hasSize(1)
                .haveExactly(1, deployment(DEPLOYER_IT_WAR));
    }

    @Test
    @InSequence(value = 300)
    public void shouldDeployJdbcDriver() throws Exception {
        String plan = ""
                + "org.postgresql:\n"
                + "  postgresql:\n"
                + "    type: jar\n"
                + "    version: \"9.4.1207\"\n";

        deployer.run(plan);

        assertThat(container.getAllDeployments())
                .hasSize(2)
                .haveExactly(1, allOf(deployment("postgresql"), checksum("f2ea471fbe4446057991e284a6b4b3263731f319")))
                .haveExactly(1, deployment(DEPLOYER_IT_WAR));
    }

    @Test
    @InSequence(value = Integer.MAX_VALUE)
    public void shouldUndeployEverything() throws Exception {
        // TODO pin DEPLOYER_IT_WAR & manage configs
        String plan = "---\n";

        deployer.run(plan);

        assertDeployments();
        // TODO assertThat(jbossConfig.read()).isEqualTo(jbossConfig.getOrig());
    }
}
