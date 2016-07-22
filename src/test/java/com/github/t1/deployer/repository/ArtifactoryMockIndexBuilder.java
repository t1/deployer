package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Checksum;
import lombok.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;
import java.util.*;

import static com.github.t1.deployer.repository.ArtifactoryMock.*;

public class ArtifactoryMockIndexBuilder {
    public static void main(String[] args) {
        new ArtifactoryMockIndexBuilder().run();
    }

    private final Map<Checksum, java.nio.file.Path> INDEX = new HashMap<>();

    @SneakyThrows(IOException.class)
    public void run() {
        System.out.println("build local maven repository checksum index");
        BuildChecksumsFileVisitor visitor = new BuildChecksumsFileVisitor(MAVEN_REPOSITORY);
        Instant start = Instant.now();

        Files.walkFileTree(MAVEN_REPOSITORY, visitor);
        writeIndex();

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("ended after " + duration.getSeconds() + "s for " + visitor.getCount() + " deployables");
    }

    @RequiredArgsConstructor
    private final class BuildChecksumsFileVisitor extends SimpleFileVisitor<java.nio.file.Path> {
        private final java.nio.file.Path root;
        @Getter
        private int count = 0;

        @Override
        public FileVisitResult visitFile(java.nio.file.Path path, BasicFileAttributes attrs) {
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
            return fileName.endsWith(".war") || fileName.endsWith(".ear") || (
                    fileName.contains("postgresql") && fileName.endsWith(".jar")
                            && !fileName.endsWith("-javadoc.jar") && !fileName.endsWith("-sources.jar")
            );
        }
    }

    @SneakyThrows(IOException.class)
    private void writeIndex() {
        try (BufferedWriter writer = Files.newBufferedWriter(MAVEN_INDEX_FILE, UTF_8)) {
            INDEX.entrySet().stream()
                 .sorted((left, right) -> left.getValue().compareTo(right.getValue()))
                 .forEach(entry -> write(writer, entry));
        }
    }

    private Writer write(BufferedWriter writer, Map.Entry<Checksum, java.nio.file.Path> entry) {
        try {
            return writer.append(entry.getKey().hexString()).append(":")
                         .append(entry.getValue().toString()).append("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
