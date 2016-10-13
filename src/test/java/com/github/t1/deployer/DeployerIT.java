package com.github.t1.deployer;

import com.github.t1.deployer.app.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.repository.ArtifactoryMockLauncher;
import com.github.t1.testtools.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.jboss.arquillian.junit.*;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.t1.deployer.app.ConfigProducer.*;
import static com.github.t1.deployer.app.DeployerBoundary.*;
import static com.github.t1.deployer.container.LogHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static com.github.t1.rest.fallback.YamlMessageBodyReader.*;
import static java.util.concurrent.TimeUnit.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

@Slf4j
@RunWith(Arquillian.class)
@SuppressWarnings("CdiInjectionPointsInspection")
public class DeployerIT {
    private static final String WILDFLY_VERSION = "10.0.0.Final";
    private static final boolean USE_ARTIFACTORY_MOCK = true;

    private static final String CONFIG_DIR = System.getProperty("jboss.server.config.dir");
    public static final Supplier<Path> ROOT_BUNDLE_PATH = () -> Paths.get(CONFIG_DIR).resolve(ROOT_BUNDLE);

    private static final DeploymentName DEPLOYER_IT = new DeploymentName("deployer-it.war");
    private static final Checksum POSTGRESQL_9_4_1207_CHECKSUM = Checksum.fromString(
            "f2ea471fbe4446057991e284a6b4b3263731f319");
    private static final Checksum JOLOKIA_1_3_1_CHECKSUM = Checksum.fromString(
            "52709CBC859E208DC8E540EB5C7047C316D9653F");
    private static final Checksum JOLOKIA_1_3_2_CHECKSUM = Checksum.fromString(
            "9E29ADD9DF1FA9540654C452DCBF0A2E47CC5330");
    public static final Checksum JOLOKIA_1_3_3_CHECKSUM = Checksum.fromString(
            "F6E5786754116CC8E1E9261B2A117701747B1259");
    public static final Checksum JOLOKIA_1_3_4_SNAPSHOT_CHECKSUM = Checksum.fromString(
            "C8BB60C0CE61C2BEEC370D9127ED340DCA5F566D");
    private static final String PLAN_JOLOKIA_WITH_VERSION_VAR = ""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    group-id: org.jolokia\n"
            + "    artifact-id: jolokia-war\n"
            + "    version: ${jolokia.version}\n";
    private static final Checksum UNKNOWN_CHECKSUM = Checksum.ofHexString("9999999999999999999999999999999999999999");

    private static Condition<DeploymentResource> deployment(String name) {
        return deployment(new DeploymentName(name));
    }

    private static Condition<DeploymentResource> deployment(DeploymentName name) {
        return new Condition<>(name::matches, "deployment with name '" + name + "'");
    }

    private static Condition<DeploymentResource> checksum(Checksum checksum) {
        return new Condition<>(deployment -> deployment.checksum().equals(checksum),
                "deployment with checksum '" + checksum + "'");
    }

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive createDeployment() {
        return new WebArchiveBuilder(DEPLOYER_IT.getValue())
                .with(DeployerBoundary.class.getPackage())
                .with(TestLoggerRule.class, FileMemento.class, LoggerMemento.class, SystemPropertiesRule.class)
                .library("org.assertj", "assertj-core")
                .print()
                .build();
    }

    static {
        if (runningOnClient()) {
            Path configFile = Paths
                    .get(System.getProperty("user.dir"))
                    .resolve("target/server/wildfly-dist_" + WILDFLY_VERSION + "/wildfly-" + WILDFLY_VERSION)
                    .resolve("standalone/configuration")
                    .resolve(DEPLOYER_CONFIG_YAML);
            write(configFile, ""
                    + "vars:\n"
                    + "  config-var: 1.3.2"
            );
            // system

            if (USE_ARTIFACTORY_MOCK)
                try {
                    // TODO can we instead deploy this? or use DropwizardClientRule?
                    new ArtifactoryMockLauncher().noConsole().run("server");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
        }
    }

    @SneakyThrows(IOException.class)
    private static void write(Path path, String contents) {
        log.info("write to {}:\n{}", path, contents);
        Files.write(path, contents.getBytes());
    }

    private static boolean runningOnClient() { return CONFIG_DIR == null; }


    @Rule
    public LoggerMemento loggerMemento = new LoggerMemento()
            .with("org.apache.http.headers", DEBUG)
            // .with("org.apache.http.wire", DEBUG)
            .with("com.github.t1.rest", DEBUG)
            .with("com.github.t1.rest.ResponseConverter", INFO)
            .with("com.github.t1.deployer", DEBUG);
    @Rule public TestLoggerRule logger = new TestLoggerRule();
    private static FileMemento jbossConfig;
    private static boolean first = true;

    @ArquillianResource URI baseUri;

    @Inject Container container;

    @Before
    public void setup() throws Exception {
        if (first && !runningOnClient()) {
            first = false;

            jbossConfig = new FileMemento(System.getProperty("jboss.server.config.dir") + "/standalone.xml").setup();
            jbossConfig.setOrig(jbossConfig.getOrig().replaceFirst(""
                    + "        <deployment name=\"" + DEPLOYER_IT + "\" runtime-name=\"" + DEPLOYER_IT + "\">\n"
                    + "            <content sha1=\"[0-9a-f]{40}\"/>\n"
                    + "        </deployment>\n", ""));
            // restore after JBoss is down
            jbossConfig.restoreOnShutdown().after(100, MILLISECONDS); // hell won't freeze over if this is too fast

            container.logHandler(console, new LogHandlerName("CONSOLE")).build().updateLevel(ALL);
            container.logger(LoggerCategory.of("com.github.t1.deployer")).level(DEBUG).build().add();

            log.info("deployables: {}", container.allDeployments());
            assertThat(theDeployments()).isEmpty();
        }
    }

    public List<Audit> post(String expectedStatus) {
        return post(expectedStatus, null, OK).readEntity(Audits.class).getAudits();
    }

    @SneakyThrows(IOException.class)
    public Response post(String plan, Entity<?> entity, Status expectedStatus) {
        try (FileMemento memento = new FileMemento(ROOT_BUNDLE_PATH).setup()) {
            memento.write(plan);

            Response response = ClientBuilder
                    .newClient()
                    .target(baseUri)
                    .request(APPLICATION_JSON_TYPE)
                    .buildPost(entity)
                    .invoke();
            assertThat(response.getStatusInfo()).isEqualTo(expectedStatus);
            return response;
        }
    }


    private Stream<DeploymentResource> theDeployments() {
        return container.allDeployments().stream().filter(deployment -> !DEPLOYER_IT.matches(deployment));
    }

    @Test
    @InSequence(value = 100)
    public void shouldFailToDeployWebArchiveWithUnknownVersion() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia-war:\n"
                + "    group-id: org.jolokia\n"
                + "    version: 9999\n";

        Response response = post(plan, null, NOT_FOUND);

        assertThat(theDeployments()).isEmpty();
        assertThat(response.readEntity(String.class))
                .contains("not in repository")
                .contains("org.jolokia:jolokia-war:9999:war");
    }

    @Test
    @InSequence(value = 150)
    public void shouldFailToDeployWebArchiveWithIncorrectChecksum() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.1\n"
                + "    checksum: " + UNKNOWN_CHECKSUM;

        String detail = post(plan, null, BAD_REQUEST).readEntity(String.class);

        assertThat(theDeployments()).isEmpty();
        assertThat(detail).contains("Repository checksum [52709cbc859e208dc8e540eb5c7047c316d9653f] "
                + "does not match planned checksum [" + UNKNOWN_CHECKSUM + "]");
    }

    @Test
    @InSequence(value = 200)
    public void shouldDeployWebArchiveWithCorrectChecksum() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.1\n"
                + "    checksum: 52709cbc859e208dc8e540eb5c7047c316d9653f";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).haveExactly(1, deployment("jolokia.war", JOLOKIA_1_3_1_CHECKSUM));
        assertThat(audits).containsExactly(
                Audit.DeployableAudit.builder().name("jolokia")
                                     .change("group-id", null, "org.jolokia")
                                     .change("artifact-id", null, "jolokia-war")
                                     .change("version", null, "1.3.1")
                                     .change("type", null, "war")
                                     .change("checksum", null, JOLOKIA_1_3_1_CHECKSUM)
                                     .added());
    }

    @Test
    @InSequence(value = 300)
    public void shouldNotUpdateWebArchiveWithSameVersion() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.1\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).haveExactly(1, deployment("jolokia.war", JOLOKIA_1_3_1_CHECKSUM));
        assertThat(audits).isEmpty();
    }

    @Test
    @InSequence(value = 350)
    public void shouldFailToUpdateWebArchiveWithWrongChecksum() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.2\n"
                + "    checksum: " + UNKNOWN_CHECKSUM;

        String detail = post(plan, null, BAD_REQUEST).readEntity(String.class);

        assertThat(detail).contains("Repository checksum [9e29add9df1fa9540654c452dcbf0a2e47cc5330] "
                + "does not match planned checksum [" + UNKNOWN_CHECKSUM + "]");
    }

    @Test
    @InSequence(value = 400)
    public void shouldUpdateWebArchiveWithConfiguredVariable() throws Exception {
        List<Audit> audits = post(PLAN_JOLOKIA_WITH_VERSION_VAR.replace("${jolokia.version}", "${config-var}"));

        logger.log("verify deployments");
        assertThat(theDeployments()).haveExactly(1, deployment("jolokia.war", JOLOKIA_1_3_2_CHECKSUM));
        logger.log("verify audits: " + audits);
        assertThat(audits).containsExactly(
                Audit.DeployableAudit.builder().name("jolokia")
                                     .change("checksum", JOLOKIA_1_3_1_CHECKSUM, JOLOKIA_1_3_2_CHECKSUM)
                                     .change("version", "1.3.1", "1.3.2")
                                     .changed());
    }

    @Test
    @InSequence(value = 500)
    public void shouldUpdateWebArchiveWithPostParameter() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: ${jolokia.version}\n";

        Entity<String> entity = Entity.json("{\"jolokia.version\":\"1.3.3\"}");
        Audits audits = post(plan, entity, OK).readEntity(Audits.class);

        logger.log("verify deployments");
        assertThat(theDeployments()).haveExactly(1, deployment("jolokia.war", JOLOKIA_1_3_3_CHECKSUM));
        logger.log("verify audits: " + audits.getAudits());
        assertThat(audits.getAudits()).containsExactly(
                Audit.DeployableAudit.builder().name("jolokia")
                                     .change("checksum", JOLOKIA_1_3_2_CHECKSUM, JOLOKIA_1_3_3_CHECKSUM)
                                     .change("version", "1.3.2", "1.3.3")
                                     .changed());
    }

    @Test
    @InSequence(value = 600)
    public void shouldFailToOverwriteVariableWithPostParameter() throws Exception {
        String plan = PLAN_JOLOKIA_WITH_VERSION_VAR.replace("${jolokia.version}", "${config-var}");
        Entity<String> entity = Entity.json("{\"config-var\":\"1.3.3\"}");
        String detail = post(plan, entity, BAD_REQUEST).readEntity(String.class);

        assertThat(detail).contains("Variable named [config-var] already set. It's not allowed to overwrite.");
    }

    @Test
    @Ignore("sending */* behaves different from sending no Content-Type header at all... but how should we do that?")
    @InSequence(value = 800)
    public void shouldNotAcceptPostWildcardWithBody() throws Exception {
        Entity<String> entity = Entity.entity("non-empty", WILDCARD_TYPE);
        String detail = post(PLAN_JOLOKIA_WITH_VERSION_VAR, entity, BAD_REQUEST).readEntity(String.class);

        assertThat(detail).contains("Please specify a `Content-Type` header when sending a body.");
    }

    @Test
    @InSequence(value = 810)
    public void shouldAcceptJsonBody() throws Exception {
        Entity<String> entity = Entity.json("{\"jolokia.version\":\"1.3.3\"}");
        List<Audit> audits = post(PLAN_JOLOKIA_WITH_VERSION_VAR, entity, OK).readEntity(Audits.class).getAudits();

        assertThat(theDeployments()).haveExactly(1, deployment("jolokia.war", JOLOKIA_1_3_3_CHECKSUM));
        assertThat(audits).isEmpty();
    }

    @Test
    @InSequence(value = 820)
    public void shouldAcceptYamlBody() throws Exception {
        Entity<String> entity = Entity.entity("jolokia.version: 1.3.3\n", APPLICATION_YAML_TYPE);
        List<Audit> audits = post(PLAN_JOLOKIA_WITH_VERSION_VAR, entity, OK).readEntity(Audits.class).getAudits();

        assertThat(theDeployments()).haveExactly(1, deployment("jolokia.war", JOLOKIA_1_3_3_CHECKSUM));
        assertThat(audits).isEmpty();
    }

    @Test
    @InSequence(value = 830)
    public void shouldAcceptFormBody() throws Exception {
        Entity<Form> entity = Entity.form(new Form("jolokia.version", "1.3.3"));
        List<Audit> audits = post(PLAN_JOLOKIA_WITH_VERSION_VAR, entity, OK).readEntity(Audits.class).getAudits();

        assertThat(theDeployments()).haveExactly(1, deployment("jolokia.war", JOLOKIA_1_3_3_CHECKSUM));
        assertThat(audits).isEmpty();
    }

    @Test
    @InSequence(value = 900)
    public void shouldUndeployWebArchive() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: xxx\n"
                + "    state: undeployed\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).isEmpty();
        assertThat(audits).containsExactly(
                Audit.DeployableAudit.builder().name("jolokia")
                                     .change("group-id", "org.jolokia", null)
                                     .change("artifact-id", "jolokia-war", null)
                                     .change("version", "1.3.3", null)
                                     .change("type", "war", null)
                                     .change("checksum", JOLOKIA_1_3_3_CHECKSUM, null)
                                     .removed());
    }

    @Test
    @InSequence(value = 950)
    public void shouldDeploySnapshotWebArchive() throws Exception {
        assumeTrue(USE_ARTIFACTORY_MOCK);

        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.4-SNAPSHOT\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).haveExactly(1, deployment("jolokia.war", JOLOKIA_1_3_4_SNAPSHOT_CHECKSUM));
        assertThat(audits).containsExactly(
                Audit.DeployableAudit.builder().name("jolokia")
                                     .change("group-id", null, "org.jolokia")
                                     .change("artifact-id", null, "jolokia-war")
                                     .change("version", null, "1.3.4-SNAPSHOT")
                                     .change("type", null, "war")
                                     .change("checksum", null, JOLOKIA_1_3_4_SNAPSHOT_CHECKSUM)
                                     .added());
    }

    @Test
    @InSequence(value = 960)
    public void shouldUndeploySnapshotWebArchive() throws Exception {
        assumeTrue(USE_ARTIFACTORY_MOCK);

        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: xxx\n"
                + "    state: undeployed\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).isEmpty();
        assertThat(audits).containsExactly(
                Audit.DeployableAudit.builder().name("jolokia")
                                     .change("group-id", "org.jolokia", null)
                                     .change("artifact-id", "jolokia-war", null)
                                     .change("version", "1.3.4-SNAPSHOT", null)
                                     .change("type", "war", null)
                                     .change("checksum", JOLOKIA_1_3_4_SNAPSHOT_CHECKSUM, null)
                                     .removed());
    }

    @Test
    @InSequence(value = 1000)
    public void shouldDeployJdbcDriver() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  postgresql:\n"
                + "    group-id: org.postgresql\n"
                + "    version: 9.4.1207\n"
                + "    type: jar\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).haveExactly(1, deployment("postgresql"));
        assertThat(audits).containsExactly(
                Audit.DeployableAudit.builder().name("postgresql")
                                     .change("group-id", null, "org.postgresql")
                                     .change("artifact-id", null, "postgresql")
                                     .change("version", null, "9.4.1207")
                                     .change("type", null, "jar")
                                     .change("checksum", null, POSTGRESQL_9_4_1207_CHECKSUM)
                                     .added());
    }

    @NotNull
    protected Condition<DeploymentResource> deployment(String name, Checksum checksum) {
        return allOf(deployment(name), checksum(checksum));
    }

    @Test
    @InSequence(value = 1090)
    public void shouldUndeployJdbcDriver() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  postgresql:\n"
                + "    group-id: org.postgresql\n"
                + "    version: 9.4.1207\n"
                + "    state: undeployed\n"
                + "    type: jar\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).isEmpty();
        assertThat(audits).containsExactly(
                Audit.DeployableAudit.builder().name("postgresql")
                                     .change("group-id", "org.postgresql", null)
                                     .change("artifact-id", "postgresql", null)
                                     .change("version", "9.4.1207", null)
                                     .change("type", "jar", null)
                                     .change("checksum", POSTGRESQL_9_4_1207_CHECKSUM, null)
                                     .removed());
    }

    // TODO shouldUpdateDeployer (WOW!)

    @Test
    @InSequence(value = Integer.MAX_VALUE)
    public void shouldUndeployEverything() throws Exception {
        // TODO pin DEPLOYER_IT_WAR & manage configs
        String plan = "---\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).isEmpty();
        if (plan.isEmpty()) { // TODO make this run
            assertThat(jbossConfig.read()).isEqualTo(jbossConfig.getOrig());
            assertThat(audits).containsExactly(
                    Audit.DeployableAudit.builder().name("postgresql")
                                         .change("group-id", "org.postgresql", null)
                                         .change("artifact-id", "postgresql", null)
                                         .change("version", "9.4.1207", null)
                                         .change("type", "jar", null)
                                         .added());
        }
    }
}
