package com.github.t1.deployer.app.file;

import static com.github.t1.deployer.container.DeploymentContainer.*;
import static java.lang.Boolean.*;
import static java.util.concurrent.TimeUnit.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;

import javax.annotation.*;
import javax.ejb.*;
import javax.inject.Inject;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Config.DeploymentListFileConfig;
import com.github.t1.deployer.repository.Repository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Startup
@Singleton
public class DeploymentListFile {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private class FileWatcher implements Runnable {
        private FileTime lastModified;

        @SneakyThrows(IOException.class)
        private FileTime lastModified() {
            if (!Files.exists(deploymentsList))
                return null;
            return Files.getLastModifiedTime(deploymentsList);
        }

        @Override
        public void run() {
            try {
                if (!Objects.equals(lastModified, lastModified())) {
                    boolean isNew = (lastModified == null);
                    lastModified = lastModified();
                    if (isNew)
                        // PostConstruct is too early: delay until the starup
                        // has completed
                        writeDeploymentsList();
                    else
                        updateFromList();
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiredArgsConstructor
    private static class DeploymentInfo {
        private static DeploymentInfo parse(String line) {
            int i = line.lastIndexOf(':');
            if (i < 0)
                throw new IllegalArgumentException("no ':' found in deployment info line [" + line + "]");
            ContextRoot contextRoot = new ContextRoot(line.substring(0, i));
            Version version = new Version(line.substring(i + 1));
            return new DeploymentInfo(contextRoot, version);
        }

        final ContextRoot contextRoot;
        final Version version;

        DeploymentInfo(Deployment deployment) {
            this.contextRoot = deployment.getContextRoot();
            this.version = deployment.getVersion();
        }

        @Override
        public String toString() {
            return contextRoot + ":" + version;
        }
    }

    @Inject
    DeploymentContainer container;
    @Inject
    Repository repository;
    @Inject
    DeploymentListFileConfig config;

    private final Path configDir = Paths.get(System.getProperty("jboss.server.config.dir", "."));
    private final Path deploymentsList = configDir.resolve("deployments.properties");

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    void start() {
        log.info("start file watcher");
        executor.scheduleWithFixedDelay(new FileWatcher(), 10, 1, SECONDS);
    }

    @PreDestroy
    void stop() {
        log.info("stop file watcher");
        executor.shutdown();
    }

    private void updateFromList() {
        log.info("deployment list file has changed");
        try {
            // FIXME User.setCurrent(new User("-file").withPrivilege("deploy",
            // "redeploy", "undeploy"));

            Map<ContextRoot, Version> expected = readDeploymentsListFile();
            for (Deployment actual : deployments()) {
                ContextRoot contextRoot = actual.getContextRoot();
                Version expectedVersion = expected.get(contextRoot);
                if (expectedVersion == null) {
                    if (TRUE == config.autoUndeploy()) {
                        log.info("expected version of {} is null -> undeploy", contextRoot);
                        container.undeploy(actual.getName());
                    } else
                        log.info("expected version of {} is null -> would undeploy but autoUndeploy is disabled",
                                contextRoot);
                } else if (expectedVersion.equals(actual.getVersion()))
                    log.debug("expected version of {} equals actual {} -> skip", contextRoot, expectedVersion);
                // already the expected version
                else {
                    log.info("version of {} changed from {} to {} -> redeploy", //
                            contextRoot, actual.getVersion(), expectedVersion);
                    CheckSum checksum = repository.getChecksumForVersion(actual, expectedVersion);
                    redeploy(repository.getByChecksum(checksum));
                }
            }
        } finally {
            // User.setCurrent(null);
        }
    }

    @SneakyThrows(IOException.class)
    private Map<ContextRoot, Version> readDeploymentsListFile() {
        Map<ContextRoot, Version> out = new HashMap<>();
        for (String line : Files.readAllLines(deploymentsList, UTF_8)) {
            if (line.trim().isEmpty() || line.startsWith("#"))
                continue;
            DeploymentInfo info = DeploymentInfo.parse(line);
            out.put(info.contextRoot, info.version);
        }
        log.trace("deployments list: {}", out);
        return out;
    }

    private void redeploy(Deployment newDeployment) {
        try (InputStream inputStream = repository.getArtifactInputStream(newDeployment.getCheckSum())) {
            container.redeploy(newDeployment.getName(), inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeDeploymentsList() {
        log.info("write deployments list");
        try (Writer writer = Files.newBufferedWriter(deploymentsList, UTF_8)) {
            for (Deployment deployment : deployments())
                writer.write(new DeploymentInfo(deployment) + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Deployment> deployments() {
        List<Deployment> out = new ArrayList<>();
        for (Deployment deployment : container.getAllDeployments()) {
            ContextRoot contextRoot = deployment.getContextRoot();
            if (UNDEFINED_CONTEXT_ROOT.equals(contextRoot))
                continue;
            Deployment versioned = repository.getByChecksum(deployment.getCheckSum());
            if (versioned == null)
                continue;
            out.add(deployment.withVersion(versioned.getVersion()));
        }
        return out;
    }
}
