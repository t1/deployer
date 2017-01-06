package com.github.t1.deployer.testtools;

import com.github.t1.log.LogLevel;
import com.github.t1.xml.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.rules.ExternalResource;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.zip.*;

import static com.github.t1.testtools.FileMemento.*;
import static java.nio.file.Files.*;
import static java.nio.file.StandardCopyOption.*;
import static java.time.temporal.ChronoUnit.*;
import static org.jboss.as.controller.client.helpers.ClientConstants.*;
import static org.jboss.as.controller.client.helpers.Operations.*;

@Slf4j
public class WildflyContainerTestRule extends ExternalResource {
    private static final Path LOCAL_REPOSITORY = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");

    private final String version;
    private final URI baseUri = URI.create("http://localhost:8080");
    private final URI cliUri = URI.create("http-remoting://localhost:9990");
    private final Xml xml;
    private final XmlElement logging;

    private Process containerProcess;


    public WildflyContainerTestRule(String version) {
        this.version = version;

        install();

        this.xml = Xml.load(configFile().toUri());
        this.logging = xml.getXPathElement("/server/profile/subsystem[1]");
        logging.getXPathElement("console-handler/level").setAttribute("name", "DEBUG");
    }


    public Path home() { return Paths.get(System.getProperty("user.dir")).resolve("target/container"); }

    public Path deploymentsDir() { return home().resolve("standalone/deployments"); }

    public Path configDir() { return home().resolve("standalone/configuration"); }

    public Path configFile() { return configDir().resolve("standalone.xml"); }

    public URI baseUri() { return baseUri; }

    public WildflyContainerTestRule withLogger(String category, LogLevel level) {
        if (logging.find("logger[@category='" + category + "']").isEmpty())
            logging.addElement("logger").setAttribute("category", category)
                   .addElement("level").setAttribute("name", level.name());
        return this;
    }

    public WildflyContainerTestRule withSystemProperty(String name, String value) {
        if (xml.find("/server/system-properties/property[@name='" + name + "']").isEmpty())
            xml.getOrCreateElement("system-properties", Xml.before("management"))
               .addElement("property").setAttribute("name", name).setAttribute("value", value);
        return this;
    }


    @SneakyThrows({ IOException.class, InterruptedException.class })
    @Override protected void before() throws Throwable {
        xml.save();
        log.info("\n================================================================== start container in {}", home());
        containerProcess = new ProcessBuilder(home().resolve("bin/standalone.sh").toString())
                .directory(home().toFile())
                .redirectErrorStream(true).inheritIO()
                .start();
        Thread.sleep(1000); // give it a chance to die fast
        if (!containerProcess.isAlive())
            throw new IllegalStateException("container not started");
        log.info("container started");
    }

    @Override protected void after() {
        log.info("\n================================================================== shutdown");
        try {
            execute(Operations.createOperation("shutdown", new ModelNode().setEmptyList()));
        } catch (RuntimeException e) {
            containerProcess.destroyForcibly();
        }
        log.info("\n================================================================== shutdown done");
    }

    public ModelNode execute(ModelNode request) {
        return retryConnect("connect to cli", () -> {
            try (ModelControllerClient client = createModelControllerClient()) {
                ModelNode result = client.execute(request);
                if (isSuccessfulOutcome(result))
                    return result.get(RESULT);
                log.debug("non-successful outcome while connecting to cli: {}", result.get(OUTCOME));
                return null;
            }
        }, Objects::nonNull);
    }

    private ModelControllerClient createModelControllerClient() throws UnknownHostException {
        String host = cliUri.getHost();
        int port = cliUri.getPort();
        log.debug("create ModelControllerClient {}://{}:{}", cliUri.getScheme(), host, port);
        return ModelControllerClient.Factory.create(cliUri.getScheme(), host, port);
    }


    @SneakyThrows(InterruptedException.class)
    public <T> T retryConnect(String description, Callable<T> body, Predicate<T> finished) {
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
                log.debug("IOException in {}: {}", description, e.getMessage());
            }
            Thread.sleep(1000);
        }
        throw new RuntimeException(
                "container didn't start within " + start.until(Instant.now(), MILLIS) + " ms for " + description);
    }

    public boolean isConnectException(Exception e) {
        return e.getCause() instanceof ConnectException
                && (e.getCause().getMessage()
                     .contains("WFLYPRT0053: Could not connect to " + cliUri + ". The connection failed")
                            || e.getCause().getMessage().contains("Connection refused (Connection refused)"));
    }

    @SneakyThrows(IOException.class) private void install() {
        if (exists(home()))
            return;
        log.info("\n================================================================== download container");
        download();

        log.info("\n================================================================== unpack container");
        Path zip = LOCAL_REPOSITORY
                .resolve("org/wildfly/wildfly-dist").resolve(version)
                .resolve("wildfly-dist-" + version + ".zip");
        unzip(zip, home().getParent());
        move(home().getParent().resolve("wildfly-" + version), home());
    }


    @SneakyThrows(InterruptedException.class)
    private void download() throws IOException {
        int exitCode = new ProcessBuilder("mvn",
                "dependency:get",
                "-D" + "transitive=false",
                "-D" + "groupId=org.wildfly",
                "-D" + "artifactId=wildfly-dist",
                "-D" + "packaging=zip",
                "-D" + "version=" + version)
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

    @SneakyThrows({ IOException.class, InterruptedException.class })
    public void deploy(WebArchive war) {
        log.info("\n================================================================== deploy {}", war.getName());
        InputStream deployment = war.as(ZipExporter.class).exportAsInputStream();
        Path failedMarker = deploymentsDir().resolve(war.getName() + ".failed");
        Path deployedMarker = deploymentsDir().resolve(war.getName() + ".deployed");
        copy(deployment, deploymentsDir().resolve(war.getName()), REPLACE_EXISTING);
        Instant start = Instant.now();
        Instant timeout = start.plus(1, MINUTES);
        while (!exists(deployedMarker)) {
            if (Instant.now().isAfter(timeout))
                throw new IllegalStateException("timeout deploy");
            if (exists(failedMarker))
                throw new IllegalStateException("failed to deploy: " + readFile(failedMarker));
            Thread.sleep(100);
        }
        log.info("\n================================================================== deployed {} after {} ms",
                war.getName(), start.until(Instant.now(), MILLIS));
        Thread.sleep(5000);
    }
}
