package com.github.t1.deployer;

import com.github.t1.deployer.app.*;
import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.ArtifactoryMockLauncher;
import com.github.t1.log.LogLevel;
import com.github.t1.testtools.FileMemento;
import com.github.t1.testtools.*;
import com.github.t1.xml.Xml;
import com.github.t1.xml.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.zip.*;

import static com.github.t1.deployer.TestData.*;
import static com.github.t1.deployer.app.ConfigProducer.*;
import static com.github.t1.deployer.app.DeployerBoundary.*;
import static com.github.t1.deployer.container.Container.*;
import static com.github.t1.deployer.container.DeploymentResource.*;
import static com.github.t1.deployer.container.LogHandlerResource.*;
import static com.github.t1.deployer.container.ModelControllerClientProducer.*;
import static com.github.t1.deployer.model.LogHandlerPlan.*;
import static com.github.t1.deployer.model.LogHandlerType.*;
import static com.github.t1.deployer.model.Password.*;
import static com.github.t1.deployer.model.ProcessState.*;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.*;
import static com.github.t1.log.LogLevel.*;
import static com.github.t1.rest.fallback.YamlMessageBodyReader.*;
import static com.github.t1.testtools.FileMemento.*;
import static com.github.t1.xml.Xml.*;
import static java.nio.file.Files.*;
import static java.nio.file.StandardCopyOption.*;
import static java.time.temporal.ChronoUnit.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.*;
import static org.hibernate.validator.internal.util.CollectionHelper.*;
import static org.jboss.as.controller.client.helpers.ClientConstants.*;
import static org.jboss.as.controller.client.helpers.Operations.*;
import static org.junit.Assume.*;

@Slf4j
@RunWith(OrderedJUnitRunner.class)
public class DeployerIT {
    private static final String WILDFLY_VERSION = "10.1.0.Final";
    private static final boolean USE_ARTIFACTORY_MOCK = true;

    private static final String DEPLOYER_WAR = "deployer.war";
    private static final Path LOCAL_REPOSITORY = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");
    private static final Path WILDFLY_ZIP = LOCAL_REPOSITORY
            .resolve("org/wildfly/wildfly-dist").resolve(WILDFLY_VERSION)
            .resolve("wildfly-dist-" + WILDFLY_VERSION + ".zip");
    private static final Path CONTAINER_HOME = Paths.get(System.getProperty("user.dir")).resolve("target/container");
    private static final Path DEPLOYMENTS = CONTAINER_HOME.resolve("standalone/deployments");
    private static final Path FAILED_MARKER = DEPLOYMENTS.resolve(DEPLOYER_WAR + ".failed");
    private static final Path DEPLOYED_MARKER = DEPLOYMENTS.resolve(DEPLOYER_WAR + ".deployed");
    private static final Path CONFIG_DIR = CONTAINER_HOME.resolve("standalone/configuration");
    public static final Path JBOSS_CONFIG = CONFIG_DIR.resolve("standalone.xml");
    private static final Supplier<Path> ROOT_BUNDLE_PATH = () -> CONFIG_DIR.resolve(ROOT_BUNDLE);

    private static final URI BASE_URI = URI.create("http://localhost:8080/deployer");
    private static final URI CLI_URI = URI.create("http-remoting://localhost:9990");

    private static final Checksum UNKNOWN_CHECKSUM = Checksum.ofHexString("9999999999999999999999999999999999999999");

    private static final String PLAN_JOLOKIA_WITH_VERSION_VAR = ""
            + "deployables:\n"
            + "  jolokia:\n"
            + "    group-id: org.jolokia\n"
            + "    artifact-id: jolokia-war\n"
            + "    version: ${jolokia.version}\n";
    private static final String POSTGRESQL = ""
            + "deployables:\n"
            + "  postgresql:\n"
            + "    group-id: org.postgresql\n"
            + "    version: 9.4.1207\n"
            + "    type: jar\n";
    private static final String FOO_DATASOURCE = ""
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:test\n"
            + "    jndi-name: java:/datasources/TestDS\n"
            + "    driver: h2\n"
            + "    user-name: joe\n"
            + "    password: secret\n"
            + "    pool:\n"
            + "      min: 0\n"
            + "      initial: 1\n"
            + "      max: 10\n";

    private static final Client HTTP = ClientBuilder.newClient();

    @BeforeClass
    public static void startup() {
        startArtifactoryMock();
        downloadContainer();
        writeDeployerConfig();
        setupJBossConfig();
        startContainer();
        deployDeployer();
        readJBossConfig(); // after startup & deploy, so the container did format and order the file
    }

    @AfterClass
    public static void shutdown() {
        log.info("\n================================================================== shutdown");
        try {
            execute(Operations.createOperation("shutdown", new ModelNode().setEmptyList()));
        } catch (RuntimeException e) {
            containerProcess.destroyForcibly();
        }
        log.info("\n================================================================== shutdown done");
    }

    private static ModelNode execute(ModelNode request) {
        return retryConnect("connect to cli", () -> {
            try (ModelControllerClient client = createModelControllerClient(CLI_URI)) {
                ModelNode result = client.execute(request);
                if (isSuccessfulOutcome(result))
                    return result.get(RESULT);
                log.debug("non-successful outcome while connecting to cli: {}", result.get(OUTCOME));
                return null;
            }
        }, Objects::nonNull);
    }


    @SneakyThrows(InterruptedException.class)
    private static <T> T retryConnect(String description, Callable<T> body, Predicate<T> finished) {
        Instant start = Instant.now();
        for (int i = 0; i < 30; i++) {
            log.debug("try to {}: {}", description, i);
            try {
                T result = body.call();
                if (finished.test(result))
                    return result;
                log.debug("failed test {} for {}", i, description);
            } catch (Exception e) {
                if (!isConnectException(e))
                    throw new RuntimeException(e);
                log.debug("IOException in: {}", description);
            }
            Thread.sleep(1000);
        }
        throw new RuntimeException(
                "container didn't start within " + start.until(Instant.now(), MILLIS) + " ms for " + description);
    }

    private static boolean isConnectException(Exception e) {
        return e.getCause() instanceof ConnectException
                && (e.getCause().getMessage()
                     .contains("WFLYPRT0053: Could not connect to " + CLI_URI + ". The connection failed")
                            || e.getCause().getMessage().contains("Connection refused (Connection refused)"));
    }

    @SneakyThrows(IOException.class)
    private static void downloadContainer() {
        if (exists(CONTAINER_HOME))
            return;
        log.info("\n================================================================== download container");
        download();

        log.info("\n================================================================== unpack container");
        unzip(WILDFLY_ZIP, CONTAINER_HOME.getParent());
        move(CONTAINER_HOME.getParent().resolve("wildfly-" + WILDFLY_VERSION), CONTAINER_HOME);
    }

    @SneakyThrows(InterruptedException.class)
    private static void download() throws IOException {
        int exitCode = new ProcessBuilder("mvn",
                "dependency:get",
                "-D" + "transitive=false",
                "-D" + "groupId=org.wildfly",
                "-D" + "artifactId=wildfly-dist",
                "-D" + "packaging=zip",
                "-D" + "version=" + WILDFLY_VERSION)
                .inheritIO()
                .start()
                .waitFor();
        if (exitCode != 0)
            throw new IllegalStateException("mvn didn't return normally but returned " + exitCode);
    }

    public static void unzip(Path zip, Path target) throws IOException {
        Instant start = Instant.now();

        try (ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry zipEntry = zipStream.getNextEntry(); zipEntry != null; zipEntry = zipStream.getNextEntry()) {
                Path path = target.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    createDirectories(path);
                } else {
                    copy(zipStream, path);
                    setLastModifiedTime(path, zipEntry.getLastModifiedTime());
                    if (path.toString().endsWith(".sh"))
                        setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
                }
                zipStream.closeEntry();
            }
        }

        log.debug("unzip done after {} ms", start.until(Instant.now(), MILLIS));
    }

    private static void writeDeployerConfig() {
        writeFile(CONFIG_DIR.resolve(DEPLOYER_CONFIG_YAML), ""
                + "vars:\n"
                + "  config-var: 1.3.2\n"
                + "pin:\n"
                + "  deployables: [deployer]\n"
                + "  log-handlers: [CONSOLE, FILE]\n"
                + "  loggers: [org.jboss.as.config, sun.rmi, com.arjuna, com.github.t1.deployer, "
                + "org.apache.http.headers, org.apache.http.wire, "
                + "com.github.t1.rest, com.github.t1.rest.ResponseConverter]\n"
                + "  data-sources: [ExampleDS]\n"
                + "manage: [all]\n"
                + "triggers: [post]\n"
        );
    }

    private static void startArtifactoryMock() {
        if (USE_ARTIFACTORY_MOCK)
            try {
                // TODO can we instead deploy this? or use DropwizardClientRule?
                new ArtifactoryMockLauncher().noConsole().run("server");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }


    private static void setupJBossConfig() {
        Xml xml = Xml.load(JBOSS_CONFIG.toUri());
        XmlElement logging = xml.getXPathElement("/server/profile/subsystem[1]");
        logging.getXPathElement("console-handler/level")
               .setAttribute("name", "DEBUG");
        setLogger(logging, "org.apache.http.headers", DEBUG);
        // setLogger(logging, "org.apache.http.wire", DEBUG);
        // setLogger(logging, "com.github.t1.rest", DEBUG);
        // setLogger(logging, "com.github.t1.rest.ResponseConverter", INFO);
        setLogger(logging, "com.github.t1.deployer", DEBUG);
        setSystemProperty(xml, CLI_DEBUG, "true");
        // setSystemProperty(xml, IGNORE_SERVER_RELOAD, "true");
        xml.save();
    }

    private static void setLogger(XmlElement logging, String category, LogLevel level) {
        if (logging.find("logger[@category='" + category + "']").isEmpty())
            logging.addElement("logger").setAttribute("category", category)
                   .addElement("level").setAttribute("name", level.name());
    }

    private static void setSystemProperty(Xml xml, String name, String value) {
        if (xml.find("/server/system-properties/property[@name='" + name + "']").isEmpty())
            xml.getOrCreateElement("system-properties", before("management"))
               .addElement("property").setAttribute("name", name).setAttribute("value", value);
    }

    @SneakyThrows({ IOException.class, InterruptedException.class })
    private static void startContainer() {
        log.info("\n================================================================== start container in "
                + CONTAINER_HOME);
        containerProcess = new ProcessBuilder(CONTAINER_HOME.resolve("bin/standalone.sh").toString())
                .directory(CONTAINER_HOME.toFile())
                .redirectErrorStream(true).inheritIO()
                .start();
        Thread.sleep(1000);
        if (!containerProcess.isAlive())
            throw new IllegalStateException("container not started");
        log.info("container started");
    }

    @SneakyThrows({ IOException.class, InterruptedException.class })
    private static void deployDeployer() {
        log.info("\n================================================================== deploy deployer");
        InputStream deployment = deployment().as(ZipExporter.class).exportAsInputStream();
        copy(deployment, DEPLOYMENTS.resolve(DEPLOYER_WAR), REPLACE_EXISTING);
        Instant start = Instant.now();
        Instant timeout = start.plus(1, MINUTES);
        while (!exists(DEPLOYED_MARKER)) {
            if (Instant.now().isAfter(timeout))
                throw new IllegalStateException("timeout deploy");
            if (exists(FAILED_MARKER))
                throw new IllegalStateException("failed to deploy: " + readFile(FAILED_MARKER));
            Thread.sleep(100);
        }
        log.info("\n================================================================== deployed deployer after {} ms",
                start.until(Instant.now(), MILLIS));
        Thread.sleep(5000);
        assertThat(theDeployments()).isEmpty();
    }

    private static WebArchive deployment() {
        return new WebArchiveBuilder(DEPLOYER_WAR)
                .with(DeployerBoundary.class.getPackage())
                .library("com.github.t1", "problem-detail")
                .print()
                .build();
    }

    private static void readJBossConfig() {
        //noinspection resource
        jbossConfig = new FileMemento(JBOSS_CONFIG).setup();
        jbossConfig.restoreOnShutdown().after(100, TimeUnit.MILLISECONDS); // hell won't freeze over if this is too fast
    }


    @Rule public TestLoggerRule logger = new TestLoggerRule();

    private static FileMemento jbossConfig;
    private static Process containerProcess;


    public List<Audit> post(String expectedStatus) {
        return post(expectedStatus, null, OK).readEntity(Audits.class).getAudits();
    }

    public Response post(String plan, Entity<?> entity, Status expectedStatus) {
        return retryConnect("post request", () -> {
            try (FileMemento memento = new FileMemento(ROOT_BUNDLE_PATH).setup()) {
                memento.write(plan);

                Response response = HTTP
                        .target(BASE_URI)
                        .request(APPLICATION_JSON_TYPE)
                        .buildPost(entity)
                        .invoke();
                assertThat(response.getStatus())
                        .as("failed: %s", new Object() {
                            @Override public String toString() { return response.readEntity(String.class); }
                        })
                        .isIn(asSet(expectedStatus.getStatusCode(), NOT_FOUND.getStatusCode()));
                return response;
            }
        }, response -> response.getStatusInfo().equals(expectedStatus));
    }

    private static Map<String, Checksum> theDeployments() {
        ModelNode response = execute(readAllDeploymentsRequest());
        Map<String, Checksum> map = new LinkedHashMap<>();
        response.asList()
                .stream()
                .map(node -> node.get("result"))
                .filter(node -> !node.get("name").asString().equals("deployer.war"))
                .forEach(node -> map.put(node.get("name").asString(), Checksum.of(hash(node))));
        return map;
    }


    @Test
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
    public void shouldDeployWebArchiveWithCorrectChecksum() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.1\n"
                + "    checksum: 52709cbc859e208dc8e540eb5c7047c316d9653f";

        Response response = post(plan, null, OK);

        assertThat(response.getHeaderString(PROCESS_STATE_HEADER)).isEqualTo(running.name());
        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_131_CHECKSUM));
        assertThat(response.readEntity(Audits.class).getAudits()).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("group-id", null, "org.jolokia")
                               .change("artifact-id", null, "jolokia-war")
                               .change("version", null, "1.3.1")
                               .change("type", null, "war")
                               .change("checksum", null, JOLOKIA_131_CHECKSUM)
                               .added());
    }

    @Test
    public void shouldNotUpdateWebArchiveWithSameVersion() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.1\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_131_CHECKSUM));
        assertThat(audits).isEmpty();
    }

    @Test
    public void shouldFailToUpdateWebArchiveWithWrongChecksum() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.2\n"
                + "    checksum: " + UNKNOWN_CHECKSUM;

        String detail = post(plan, null, BAD_REQUEST).readEntity(String.class);

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_131_CHECKSUM));
        assertThat(detail).contains("Repository checksum [" + JOLOKIA_132_CHECKSUM + "] "
                + "does not match planned checksum [" + UNKNOWN_CHECKSUM + "]");
    }

    @Test
    public void shouldUpdateWebArchiveWithConfiguredVariablePlusAddLogger() throws Exception {
        String plan = PLAN_JOLOKIA_WITH_VERSION_VAR
                .replace("${jolokia.version}", "${config-var}")
                + "loggers:\n"
                + "  foo:\n"
                + "    level: DEBUG\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_132_CHECKSUM));
        assertThat(audits).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("checksum", JOLOKIA_131_CHECKSUM, JOLOKIA_132_CHECKSUM)
                               .change("version", "1.3.1", "1.3.2")
                               .changed(),
                LoggerAudit.builder().category(LoggerCategory.of("foo"))
                           .change("level", null, DEBUG)
                           .change("use-parent-handlers", null, true)
                           .added());
    }

    @Test
    public void shouldUpdateWebArchiveWithPostParameterAndRemoveLogger() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: ${jolokia.version}\n";

        Entity<String> entity = Entity.json("{\"jolokia.version\":\"1.3.3\"}");
        Audits audits = post(plan, entity, OK).readEntity(Audits.class);

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_133_CHECKSUM));
        assertThat(audits.getAudits()).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("checksum", JOLOKIA_132_CHECKSUM, JOLOKIA_133_CHECKSUM)
                               .change("version", "1.3.2", "1.3.3")
                               .changed(),
                LoggerAudit.builder().category(LoggerCategory.of("foo"))
                           .change("level", DEBUG, null)
                           .change("use-parent-handlers", true, null)
                           .removed());
    }

    @Test
    public void shouldFailToOverwriteVariableWithPostParameter() throws Exception {
        String plan = PLAN_JOLOKIA_WITH_VERSION_VAR.replace("${jolokia.version}", "${config-var}");

        Entity<String> entity = Entity.json("{\"config-var\":\"1.3.3\"}");
        String detail = post(plan, entity, BAD_REQUEST).readEntity(String.class);

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_133_CHECKSUM));
        assertThat(detail).contains("Variable named [config-var] already set. It's not allowed to overwrite.");
    }

    // @Test
    @Ignore("sending */* behaves different from sending no Content-Type header at all... but how should we do that?")
    public void shouldNotAcceptPostWildcardWithBody() throws Exception {
        Entity<String> entity = Entity.entity("non-empty", WILDCARD_TYPE);
        String detail = post(PLAN_JOLOKIA_WITH_VERSION_VAR, entity, BAD_REQUEST).readEntity(String.class);

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_133_CHECKSUM));
        assertThat(detail).contains("Please specify a `Content-Type` header when sending a body.");
    }

    @Test
    public void shouldAcceptJsonBody() throws Exception {
        Entity<String> entity = Entity.json("{\"jolokia.version\":\"1.3.3\"}");
        List<Audit> audits = post(PLAN_JOLOKIA_WITH_VERSION_VAR, entity, OK).readEntity(Audits.class).getAudits();

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_133_CHECKSUM));
        assertThat(audits).isEmpty();
    }

    @Test
    public void shouldAcceptYamlBody() throws Exception {
        Entity<String> entity = Entity.entity("jolokia.version: 1.3.3\n", APPLICATION_YAML_TYPE);
        List<Audit> audits = post(PLAN_JOLOKIA_WITH_VERSION_VAR, entity, OK).readEntity(Audits.class).getAudits();

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_133_CHECKSUM));
        assertThat(audits).isEmpty();
    }

    @Test
    public void shouldAcceptFormBody() throws Exception {
        Entity<Form> entity = Entity.form(new Form("jolokia.version", "1.3.3"));
        List<Audit> audits = post(PLAN_JOLOKIA_WITH_VERSION_VAR, entity, OK).readEntity(Audits.class).getAudits();

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_133_CHECKSUM));
        assertThat(audits).isEmpty();
    }

    @Test
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
        assertThat(audits).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("group-id", "org.jolokia", null)
                               .change("artifact-id", "jolokia-war", null)
                               .change("version", "1.3.3", null)
                               .change("type", "war", null)
                               .change("checksum", JOLOKIA_133_CHECKSUM, null)
                               .removed());
    }

    @Test
    public void shouldDeployTwoWebArchives() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.3\n"
                + "  mockserver:\n"
                + "    group-id: org.mock-server\n"
                + "    artifact-id: mockserver-war\n"
                + "    version: 3.10.3\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).containsOnly(
                entry("jolokia.war", JOLOKIA_133_CHECKSUM),
                entry("mockserver.war", MOCKSERVER_3_10_3_CHECKSUM));
        assertThat(audits).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("group-id", null, "org.jolokia")
                               .change("artifact-id", null, "jolokia-war")
                               .change("version", null, "1.3.3")
                               .change("type", null, "war")
                               .change("checksum", null, JOLOKIA_133_CHECKSUM)
                               .added(),
                DeployableAudit.builder().name("mockserver")
                               .change("group-id", null, "org.mock-server")
                               .change("artifact-id", null, "mockserver-war")
                               .change("version", null, "3.10.3")
                               .change("type", null, "war")
                               .change("checksum", null, MOCKSERVER_3_10_3_CHECKSUM)
                               .added());
    }

    @Test
    public void shouldUpdateTwoWebArchives() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.4\n"
                + "  mockserver:\n"
                + "    group-id: org.mock-server\n"
                + "    artifact-id: mockserver-war\n"
                + "    version: 3.10.4\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).containsOnly(
                entry("jolokia.war", JOLOKIA_134_CHECKSUM),
                entry("mockserver.war", MOCKSERVER_3_10_4_CHECKSUM));
        assertThat(audits).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("checksum", JOLOKIA_133_CHECKSUM, JOLOKIA_134_CHECKSUM)
                               .change("version", "1.3.3", "1.3.4")
                               .changed(),
                DeployableAudit.builder().name("mockserver")
                               .change("checksum", MOCKSERVER_3_10_3_CHECKSUM, MOCKSERVER_3_10_4_CHECKSUM)
                               .change("version", "3.10.3", "3.10.4")
                               .changed());
    }

    @Test
    public void shouldUndeployTwoWebArchives() throws Exception {
        List<Audit> audits = post("{}");

        assertThat(theDeployments()).isEmpty();
        assertThat(audits).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("group-id", "org.jolokia", null)
                               .change("artifact-id", "jolokia-war", null)
                               .change("version", "1.3.4", null)
                               .change("type", "war", null)
                               .change("checksum", JOLOKIA_134_CHECKSUM, null)
                               .removed(),
                DeployableAudit.builder().name("mockserver")
                               .change("group-id", "org.mock-server", null)
                               .change("artifact-id", "mockserver-war", null)
                               .change("version", "3.10.4", null)
                               .change("type", "war", null)
                               .change("checksum", MOCKSERVER_3_10_4_CHECKSUM, null)
                               .removed());
    }

    @Test
    public void shouldDeploySnapshotWebArchive() throws Exception {
        assumeTrue(USE_ARTIFACTORY_MOCK);

        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: 1.3.4-SNAPSHOT\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).containsOnly(entry("jolokia.war", JOLOKIA_134_SNAPSHOT_CHECKSUM));
        assertThat(audits).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("group-id", null, "org.jolokia")
                               .change("artifact-id", null, "jolokia-war")
                               .change("version", null, "1.3.4-SNAPSHOT")
                               .change("type", null, "war")
                               .change("checksum", null, JOLOKIA_134_SNAPSHOT_CHECKSUM)
                               .added());
    }

    @Test
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
        assertThat(audits).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("group-id", "org.jolokia", null)
                               .change("artifact-id", "jolokia-war", null)
                               .change("version", "1.3.4-SNAPSHOT", null)
                               .change("type", "war", null)
                               .change("checksum", JOLOKIA_134_SNAPSHOT_CHECKSUM, null)
                               .removed());
    }

    @Test
    public void shouldDeployJdbcDriver() throws Exception {
        List<Audit> audits = post(POSTGRESQL);

        assertThat(theDeployments()).containsOnly(entry("postgresql", POSTGRESQL_9_4_1207_CHECKSUM));
        assertThat(audits).containsOnly(
                DeployableAudit.builder().name("postgresql")
                               .change("group-id", null, "org.postgresql")
                               .change("artifact-id", null, "postgresql")
                               .change("version", null, "9.4.1207")
                               .change("type", null, "jar")
                               .change("checksum", null, POSTGRESQL_9_4_1207_CHECKSUM)
                               .added());
    }

    @Test
    public void shouldDeployDataSource() throws Exception {
        String plan = ""
                + POSTGRESQL
                + "data-sources:\n"
                + FOO_DATASOURCE
                + "      max-age: 5 min\n";

        List<Audit> audits = post(plan);

        assertThat(audits).containsOnly(
                DataSourceAudit.builder().name(new DataSourceName("foo"))
                               .change("uri", null, "jdbc:h2:mem:test")
                               .change("jndi-name", null, "java:/datasources/TestDS")
                               .change("driver", null, "h2")
                               .change("user-name", null, CONCEALED)
                               .change("password", null, CONCEALED)
                               .change("pool:min", null, "0")
                               .change("pool:initial", null, "1")
                               .change("pool:max", null, "10")
                               .change("pool:max-age", null, "5 min")
                               .added());
        assertThat(definedPropertiesOf(execute(readDatasourceRequest("foo", false))))
                .has(property("connection-url", "jdbc:h2:mem:test"))
                .has(property("driver-name", "h2"))
                .has(property("enabled", "true"))
                .has(property("idle-timeout-minutes", "5"))
                .has(property("initial-pool-size", "1"))
                .has(property("jndi-name", "java:/datasources/TestDS"))
                .has(property("max-pool-size", "10"))
                .has(property("min-pool-size", "0"))
                .has(property("password", "secret"))
                .has(property("user-name", "joe"));
        assertThat(theDeployments()).containsOnly(entry("postgresql", POSTGRESQL_9_4_1207_CHECKSUM));
    }

    @Test
    public void shouldChangeDataSourceMaxAgeTo10() throws Exception {
        String plan = ""
                + POSTGRESQL
                + "data-sources:\n"
                + FOO_DATASOURCE
                + "      max-age: 600 seconds\n";
        // TODO + "    xa: true\n"

        Response response = post(plan, null, OK);

        assertThat(response.getHeaderString(PROCESS_STATE_HEADER)).isEqualTo(reloadRequired.name());
        assertThat(response.readEntity(Audits.class).getAudits()).containsOnly(
                DataSourceAudit.builder().name(new DataSourceName("foo"))
                               // TODO .change("xa", null, true)
                               .change("pool:max-age", "5 min", "10 min")
                               .changed());
        assertThat(definedPropertiesOf(execute(readDatasourceRequest("foo", false))))
                .has(property("connection-url", "jdbc:h2:mem:test"))
                .has(property("driver-name", "h2"))
                .has(property("enabled", "true"))
                .has(property("idle-timeout-minutes", "10"))
                .has(property("initial-pool-size", "1"))
                .has(property("jndi-name", "java:/datasources/TestDS"))
                .has(property("max-pool-size", "10"))
                .has(property("min-pool-size", "0"))
                .has(property("password", "secret"))
                .has(property("user-name", "joe"));
        assertThat(theDeployments()).containsOnly(entry("postgresql", POSTGRESQL_9_4_1207_CHECKSUM));
    }

    @Test
    public void shouldDeployXaDataSource() throws Exception {
        String plan = ""
                + POSTGRESQL
                + "data-sources:\n"
                + FOO_DATASOURCE
                + "      max-age: 10 min\n"
                // TODO + "    xa: true\n"
                + "  barDS:\n"
                + "    xa: true\n"
                + "    uri: jdbc:postgresql://my-db.server.lan:5432/bar\n"
                + "    user-name: joe\n"
                + "    password: secret\n"
                + "    pool:\n"
                + "      min: 0\n"
                + "      initial: 0\n"
                + "      max: 10\n"
                + "      max-age: 5 min\n";

        List<Audit> audits = post(plan);

        assertThat(audits).containsOnly(
                DataSourceAudit.builder().name(new DataSourceName("barDS"))
                               .change("uri", null, "jdbc:postgresql://my-db.server.lan:5432/bar")
                               .change("jndi-name", null, "java:/datasources/barDS")
                               .change("driver", null, "postgresql")
                               .change("xa", null, true)
                               .change("user-name", null, CONCEALED)
                               .change("password", null, CONCEALED)
                               .change("pool:min", null, "0")
                               .change("pool:initial", null, "0")
                               .change("pool:max", null, "10")
                               .change("pool:max-age", null, "5 min")
                               .added());
        assertThat(definedPropertiesOf(execute(readDatasourceRequest("barDS", true))))
                .has(property("driver-name", "postgresql"))
                .has(property("enabled", "true"))
                .has(property("idle-timeout-minutes", "5"))
                .has(property("initial-pool-size", "0"))
                .has(property("jndi-name", "java:/datasources/barDS"))
                .has(property("max-pool-size", "10"))
                .has(property("min-pool-size", "0"))
                .has(property("password", "secret"))
                .has(property("user-name", "joe"))
                .has(property("xa-datasource-properties", "{"
                        + "\"ServerName\" => {\"value\" => \"my-db.server.lan\"},"
                        + "\"PortNumber\" => {\"value\" => \"5432\"},"
                        + "\"DatabaseName\" => {\"value\" => \"bar\"}"
                        + "}"));
        assertThat(theDeployments()).containsOnly(entry("postgresql", POSTGRESQL_9_4_1207_CHECKSUM));
    }

    @Test
    public void shouldUndeployAllDataSources() throws Exception {
        List<Audit> audits = post(POSTGRESQL);

        assertThat(audits).containsOnly(
                DataSourceAudit.builder().name(new DataSourceName("barDS")) // sorted!
                               .change("uri", "jdbc:postgresql://my-db.server.lan:5432/bar", null)
                               .change("jndi-name", "java:/datasources/barDS", null)
                               .change("driver", "postgresql", null)
                               .change("xa", true, null)
                               .change("user-name", CONCEALED, null)
                               .change("password", CONCEALED, null)
                               .change("pool:min", "0", null)
                               .change("pool:initial", "0", null)
                               .change("pool:max", "10", null)
                               .change("pool:max-age", "5 min", null)
                               .removed(),
                DataSourceAudit.builder().name(new DataSourceName("foo"))
                               .change("uri", "jdbc:h2:mem:test", null)
                               .change("jndi-name", "java:/datasources/TestDS", null)
                               .change("driver", "h2", null)
                               // TODO .change("xa", true, null)
                               .change("user-name", CONCEALED, null)
                               .change("password", CONCEALED, null)
                               .change("pool:min", "0", null)
                               .change("pool:initial", "1", null)
                               .change("pool:max", "10", null)
                               .change("pool:max-age", "10 min", null)
                               .removed());
        assertThat(theDeployments()).containsOnly(entry("postgresql", POSTGRESQL_9_4_1207_CHECKSUM));
    }

    @Test
    public void shouldDeployLogHandlerAndLogger() throws Exception {
        String plan = ""
                + POSTGRESQL
                + "log-handlers:\n"
                + "  FOO:\n"
                + "    level: INFO\n"
                + "loggers:\n"
                + "  foo:\n"
                + "    handler: FOO\n";

        List<Audit> audits = post(plan);

        assertThat(audits).containsOnly(
                LogHandlerAudit.builder()
                               .type(periodicRotatingFile)
                               .name(new LogHandlerName("FOO"))
                               .change("level", null, INFO)
                               .change("file", null, "foo.log")
                               .change("suffix", null, DEFAULT_SUFFIX)
                               .added(),
                LoggerAudit.builder().category(LoggerCategory.of("foo"))
                           .change("level", null, DEBUG)
                           .change("use-parent-handlers", null, false)
                           .change("handlers", null, "[FOO]")
                           .added());
        assertThat(definedPropertiesOf(execute(readLogHandlerRequest(periodicRotatingFile, "FOO"))))
                .has(property("append", "true"))
                .has(property("autoflush", "true"))
                .has(property("enabled", "true"))
                .has(property("file", "{"
                        + "\"path\" => \"foo.log\","
                        + "\"relative-to\" => \"jboss.server.log.dir\"}"))
                .has(property("formatter", DEFAULT_LOG_FORMAT))
                .has(property("level", "INFO"))
                .has(property("name", "FOO"))
                .has(property("suffix", DEFAULT_SUFFIX));
        assertThat(definedPropertiesOf(execute(readLoggerRequest("foo"))))
                .has(property("category", "foo"))
                .has(property("handlers", "[\"FOO\"]"))
                .has(property("level", "DEBUG"))
                .has(property("use-parent-handlers", "false"));
        assertThat(theDeployments()).containsOnly(entry("postgresql", POSTGRESQL_9_4_1207_CHECKSUM));
    }

    @Test
    public void shouldUndeployLogHandler() throws Exception {
        String plan = ""
                + POSTGRESQL;

        List<Audit> audits = post(plan);

        assertThat(audits).containsOnly(
                LogHandlerAudit.builder()
                               .type(periodicRotatingFile)
                               .name(new LogHandlerName("FOO"))
                               .change("level", INFO, null)
                               .change("file", "foo.log", null)
                               .change("suffix", DEFAULT_SUFFIX, null)
                               .removed(),
                LoggerAudit.builder().category(LoggerCategory.of("foo"))
                           .change("level", DEBUG, null)
                           .change("use-parent-handlers", false, null)
                           .change("handlers", "[FOO]", null)
                           .removed());
        assertThat(theDeployments()).containsOnly(entry("postgresql", POSTGRESQL_9_4_1207_CHECKSUM));
    }

    @Test
    public void shouldDeploySecondDeployableWithOnlyOnePostParameter() throws Exception {
        String plan = ""
                + "deployables:\n"
                + "  jolokia:\n"
                + "    group-id: org.jolokia\n"
                + "    artifact-id: jolokia-war\n"
                + "    version: ${jolokia.version}\n"
                + "  postgresql:\n"
                + "    group-id: org.postgresql\n"
                + "    version: ${postgresql.version}\n"
                + "    type: jar\n";

        Entity<String> entity = Entity.json("{\"jolokia.version\":\"1.3.3\"}");
        Audits audits = post(plan, entity, OK).readEntity(Audits.class);

        assertThat(theDeployments()).containsOnly(
                entry("jolokia.war", JOLOKIA_133_CHECKSUM),
                entry("postgresql", POSTGRESQL_9_4_1207_CHECKSUM));
        assertThat(audits.getAudits()).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("group-id", null, "org.jolokia")
                               .change("artifact-id", null, "jolokia-war")
                               .change("version", null, "1.3.3")
                               .change("type", null, "war")
                               .change("checksum", null, JOLOKIA_133_CHECKSUM)
                               .added());
    }

    @Test
    public void shouldCleanUp() throws Exception {
        String plan = "---\n";

        List<Audit> audits = post(plan);

        assertThat(theDeployments()).isEmpty();
        assertThat(audits).containsOnly(
                DeployableAudit.builder().name("jolokia")
                               .change("group-id", "org.jolokia", null)
                               .change("artifact-id", "jolokia-war", null)
                               .change("version", "1.3.3", null)
                               .change("type", "war", null)
                               .change("checksum", JOLOKIA_133_CHECKSUM, null)
                               .removed(),
                DeployableAudit.builder().name("postgresql")
                               .change("group-id", "org.postgresql", null)
                               .change("artifact-id", "postgresql", null)
                               .change("version", "9.4.1207", null)
                               .change("type", "jar", null)
                               .change("checksum", POSTGRESQL_9_4_1207_CHECKSUM, null)
                               .removed());
        assertThat(jbossConfig.read()).isEqualTo(jbossConfig.getOrig());
    }
}
