package com.github.t1.deployer.app;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.Version;
import com.github.t1.deployer.repository.Repository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.*;
import javax.ejb.*;
import javax.inject.Inject;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;

import static com.github.t1.deployer.container.DeploymentContainer.*;
import static java.util.concurrent.TimeUnit.*;

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
                        // PostConstruct is too early: delay until the startup
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

        private DeploymentInfo(Deployment deployment) {
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

    private final Path configDir = Paths.get(System.getProperty("jboss.server.config.dir", "."));
    private final Path deploymentsList = configDir.resolve("deployments.properties");

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + deploymentsList;
    }

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
        Map<ContextRoot, Version> expected = readDeploymentsListFile();
        for (Deployment actual : deployments()) {
            ContextRoot contextRoot = actual.getContextRoot();
            Version expectedVersion = expected.get(contextRoot);
            if (expectedVersion == null) {
                if (contextRoot.getValue().isEmpty()) {
                    log.info("expected version of {} is null -> undeploy", contextRoot);
                    container.undeploy(actual.getName());
                } else {
                    log.info("expected version of {} is null -> would undeploy but autoUndeploy is disabled",
                            contextRoot);
                }
            } else if (expectedVersion.equals(actual.getVersion()))
                log.debug("expected version of {} equals actual {} -> skip", contextRoot, expectedVersion);
            else {
                log.info("version of {} changed from {} to {} -> redeploy", //
                        contextRoot, actual.getVersion(), expectedVersion);
                // TODO Checksum checksum = repository.getChecksumForVersion(actual, expectedVersion);
                // TODO redeploy(repository.getByChecksum(checksum));
            }
        }
        log.info("deployment list file update done");
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
        // try (InputStream inputStream = repository.getArtifactInputStream(newDeployment.getChecksum())) {
        //     container.redeploy(newDeployment.getName(), inputStream);
        // } catch (IOException e) {
        //     throw new RuntimeException(e);
        // }
    }

    public void writeDeploymentsList() {
        log.info("write deployments list");
        try (Writer writer = Files.newBufferedWriter(deploymentsList, UTF_8)) {
            List<Deployment> deployments = deployments();
            for (Deployment deployment : deployments)
                writer.write(new DeploymentInfo(deployment) + "\n");
            log.info("written deployments list with {} entries", deployments.size());
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
            // TODO Deployment versioned = repository.getByChecksum(deployment.getChecksum());
            // if (versioned == null)
            //     continue;
            // out.add(deployment.withVersion(versioned.getVersion()));
        }
        return out;
    }
}
