package com.github.t1.deployer.repository;

import static com.github.t1.deployer.repository.ArtifactoryMock.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;
import java.util.*;

import lombok.*;

import com.github.t1.deployer.model.CheckSum;

public class ArtifactoryMockIndexBuilder {
    public static void main(String[] args) {
        new ArtifactoryMockIndexBuilder().run();
    }

    private final Map<CheckSum, java.nio.file.Path> INDEX = new HashMap<>();

    @RequiredArgsConstructor
    private final class BuildChecksumsFileVisitor extends SimpleFileVisitor<java.nio.file.Path> {
        private final java.nio.file.Path root;
        @Getter
        private int count = 0;

        @Override
        public FileVisitResult visitFile(java.nio.file.Path path, BasicFileAttributes attrs) {
            if (!isDeployable(path.getFileName().toString()))
                return FileVisitResult.CONTINUE;
            CheckSum checkSum = CheckSum.sha1(path);
            java.nio.file.Path relativePath = root.relativize(path);
            // System.out.println(relativePath + " (" + count + ") -> " + checkSum);
            INDEX.put(checkSum, relativePath);
            ++count;
            return FileVisitResult.CONTINUE;
        }

        private boolean isDeployable(String fileName) {
            return fileName.endsWith(".war") || fileName.endsWith(".ear");
        }
    }

    @SneakyThrows(IOException.class)
    public void run() {
        System.out.println("build local maven repository checksum index");
        BuildChecksumsFileVisitor visitor = new BuildChecksumsFileVisitor(MAVEN_REPOSITORY);
        Instant start = Instant.now();

        Files.walkFileTree(MAVEN_REPOSITORY, visitor);
        writeIndex();

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("ended after " + duration + " for " + visitor.getCount() + " deployables");
    }

    @SneakyThrows(IOException.class)
    private void writeIndex() {
        try (BufferedWriter writer = Files.newBufferedWriter(MAVEN_INDEX_FILE, UTF_8)) {
            for (Map.Entry<CheckSum, java.nio.file.Path> entry : INDEX.entrySet()) {
                writer.append(entry.getKey().hexString()).append(":") //
                        .append(entry.getValue().toString()).append("\n");
            }
        }
    }
}
