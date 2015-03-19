package com.github.t1.deployer;

import static com.github.t1.deployer.Container.*;
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

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Startup
@Singleton
public class DeploymentsList {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private class FileWatcher implements Runnable {
        private FileTime lastModified;

        public FileWatcher() {
            this.lastModified = lastModified();
        }

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
                    lastModified = lastModified();
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
    Container container;
    @Inject
    Repository repository;

    private final Path configDir = Paths.get(System.getProperty("jboss.server.config.dir", "."));
    private final Path deploymentsList = configDir.resolve("deployments.properties");

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    void start() {
        log.debug("start file watcher");
        executor.scheduleWithFixedDelay(new FileWatcher(), 0, 1, SECONDS);
    }

    @PreDestroy
    void stop() {
        log.debug("stop file watcher");
        executor.shutdown();
    }

    private void updateFromList() {
        log.info("deployment list file has changed");
        Map<ContextRoot, Version> expected = read();
        for (Deployment actual : deployments()) {
            ContextRoot contextRoot = actual.getContextRoot();
            Version expectedVersion = expected.get(contextRoot);
            if (expectedVersion == null) {
                container.undeploy(actual.getName());
            } else if (!expectedVersion.equals(actual.getVersion())) {
                Deployment newDeployment = repository.getChecksumForVersion(actual, expectedVersion);
                newDeployment.deploy(container, repository);
            }
        }
    }

    @SneakyThrows(IOException.class)
    public Map<ContextRoot, Version> read() {
        Map<ContextRoot, Version> out = new HashMap<>();
        for (String line : Files.readAllLines(deploymentsList, UTF_8)) {
            DeploymentInfo info = DeploymentInfo.parse(line);
            out.put(info.contextRoot, info.version);
        }
        return out;
    }

    public void writeDeploymentsList() {
        log.debug("write deployments list");
        try (Writer writer = Files.newBufferedWriter(deploymentsList, UTF_8)) {
            for (Deployment deployment : deployments()) {
                writer.write(new DeploymentInfo(deployment) + "\n");
            }
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
            Deployment withVersion = repository.getByChecksum(deployment.getCheckSum());
            if (withVersion == null)
                continue;
            Version version = withVersion.getVersion();
            deployment.setVersion(version);
            out.add(deployment);
        }
        return out;
    }
}
