package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Checksum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;

import static com.github.t1.deployer.repository.ArtifactoryMock.INDEX;
import static com.github.t1.deployer.repository.ArtifactoryMock.MAVEN_REPOSITORY;
import static com.github.t1.deployer.repository.ArtifactoryMock.writeIndex;

public class ArtifactoryMockIndexBuilder {
    public static void main(String[] args) {
        new ArtifactoryMockIndexBuilder().run();
    }

    @SneakyThrows(IOException.class)
    void run() {
        System.out.println("build local maven repository checksum index");
        INDEX.clear();
        BuildChecksumsFileVisitor visitor = new BuildChecksumsFileVisitor(MAVEN_REPOSITORY);
        Instant start = Instant.now();

        Files.walkFileTree(MAVEN_REPOSITORY, visitor);
        writeIndex();

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("ended after " + duration.getSeconds() + "s for " + visitor.getCount() + " deployables");
    }

    @RequiredArgsConstructor
    private final class BuildChecksumsFileVisitor extends SimpleFileVisitor<Path> {
        private final java.nio.file.Path root;
        @Getter
        private int count = 0;

        @Override
        public FileVisitResult visitFile(java.nio.file.Path path, BasicFileAttributes attributes) {
            if (!isDeployable(path.getFileName().toString()))
                return FileVisitResult.CONTINUE;
            Checksum checksum = Checksum.sha1(path);
            java.nio.file.Path relativePath = root.relativize(path);
            // System.out.println(relativePath + " (" + count + ") -> " + checkSum);
            INDEX.put(checksum, relativePath);
            ++count;
            return FileVisitResult.CONTINUE;
        }

        private boolean isDeployable(String fileName) {
            return fileName.endsWith(".war") || fileName.endsWith(".ear") || fileName.endsWith(".bundle") || (
                    fileName.contains("postgresql") && fileName.endsWith(".jar")
                            && !fileName.endsWith("-javadoc.jar") && !fileName.endsWith("-sources.jar")
            );
        }
    }
}
