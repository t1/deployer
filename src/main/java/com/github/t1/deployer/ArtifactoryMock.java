package com.github.t1.deployer;

import static com.github.t1.deployer.ArtifactoryRepository.*;
import static java.util.Arrays.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.joda.time.*;
import org.joda.time.format.PeriodFormat;

/** If you don't have a real Artifactory Pro available, move this to main and configure the endpoint. */
@Slf4j
@Path("/artifactory")
public class ArtifactoryMock {
    private static final MediaType FILE_INFO = MediaType
            .valueOf("application/vnd.org.jfrog.artifactory.storage.FileInfo+json");
    private static final MediaType FOLDER_INFO = MediaType
            .valueOf("application/vnd.org.jfrog.artifactory.storage.FolderInfo+json");

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final java.nio.file.Path REPO_NAME = Paths.get("libs-release-local");

    private static final java.nio.file.Path MAVEN_HOME = Paths.get(System.getProperty("user.home"), ".m2");
    private static final java.nio.file.Path MAVEN_INDEX_FILE = MAVEN_HOME.resolve("checksum.index");
    private static final java.nio.file.Path MAVEN_REPOSITORY = MAVEN_HOME.resolve("repository");
    private static final Map<CheckSum, java.nio.file.Path> INDEX = new HashMap<>();

    public static final CheckSum FAILING_CHECKSUM = CheckSum.ofHexString("1111111111111111111111111111111111111111");
    public static final CheckSum AMBIGUOUS_CHECKSUM = CheckSum.ofHexString("2222222222222222222222222222222222222222");
    public static final CheckSum UNKNOWN_CHECKSUM = CheckSum.ofHexString("3333333333333333333333333333333333333333");

    public static final String FOO = "foo";
    public static final String BAR = "bar";

    public static final String FOO_WAR = FOO + ".war";
    public static final String BAR_WAR = BAR + ".war";

    public static final Version NEWEST_FOO_VERSION = new Version("1.3.10");
    public static final Version CURRENT_FOO_VERSION = new Version("1.3.1");

    public static final List<Version> FOO_VERSIONS = asList(//
            NEWEST_FOO_VERSION, //
            new Version("1.3.2"), //
            CURRENT_FOO_VERSION, //
            new Version("1.3.0"), //
            new Version("1.2.1"), //
            new Version("1.2.1-SNAPSHOT"), //
            new Version("1.2.0") //
            );

    public static final Version CURRENT_BAR_VERSION = new Version("0.3");

    public static final List<Version> BAR_VERSIONS = asList(//
            CURRENT_BAR_VERSION, //
            new Version("0.2") //
            );

    private static final class StringInputStream extends ByteArrayInputStream {
        private final String string;

        private StringInputStream(String string) {
            super(string.getBytes());
            this.string = string;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            StringInputStream that = (StringInputStream) obj;
            return this.string.equals(that.string);
        }

        @Override
        public int hashCode() {
            return string.hashCode();
        }

        @Override
        public String toString() {
            return "[" + string + "]";
        }
    }

    @Context
    UriInfo uriInfo;

    private URI base(String path) {
        return uriInfo.getBaseUriBuilder().path(ArtifactoryMock.class).path(path).build();
    }

    @GET
    @Path("/api/search/checksum")
    @Produces("application/vnd.org.jfrog.artifactory.search.ChecksumSearchResult+json")
    public String searchByChecksum(@QueryParam("sha1") CheckSum checkSum) {
        log.debug("search by checksum: {}", checkSum);
        return "{\"results\": [" + searchResultsFor(checkSum) + "]}";
    }

    private String searchResultsFor(CheckSum checkSum) {
        if (checksumFor(FOO).equals(checkSum)) {
            return fileSearchResult(FOO, CURRENT_FOO_VERSION);
        } else if (checksumFor(BAR).equals(checkSum)) {
            return fileSearchResult(BAR, CURRENT_BAR_VERSION);
        } else if (FAILING_CHECKSUM.equals(checkSum)) {
            throw new RuntimeException("error in repo");
        } else if (AMBIGUOUS_CHECKSUM.equals(checkSum)) {
            return fileSearchResult("x", new Version("1.0")) + "," + fileSearchResult("y", new Version("2.0"));
        } else if (UNKNOWN_CHECKSUM.equals(checkSum)) {
            return "";
        } else if (isIndexed(checkSum)) {
            java.nio.file.Path path = index().get(checkSum);
            return fileSearchResult(REPO_NAME + "/" + path);
        } else {
            String name = nameFor(checkSum);
            Version version = versionFor(checkSum);
            log.debug("fake search result for {}: {}@{}", checkSum, name, version);
            return fileSearchResult(name, version);
        }
    }

    public static CheckSum checksumFor(String name) {
        return checksumFor(name, versionFor(name));
    }

    public static Version versionFor(String name) {
        switch (name) {
            case FOO:
                return CURRENT_FOO_VERSION;
            case BAR:
                return CURRENT_BAR_VERSION;
            default:
                return versionFor(name.getBytes());
        }
    }

    public static CheckSum checksumFor(String name, Version version) {
        CheckSum result = CheckSum.sha1((name + "@" + version).getBytes()); // arbitrary but fixed for name/version
        result.getBytes()[0] = (byte) 0xFA;
        result.getBytes()[1] = (byte) 0xCE;
        result.getBytes()[2] = (byte) 0x00;
        result.getBytes()[3] = (byte) 0x00;
        return result;
    }

    private String fileSearchResult(String name, Version version) {
        return fileSearchResult(pathFor(name, version).toString());
    }

    public static java.nio.file.Path pathFor(String name, Version version) {
        return REPO_NAME.resolve(name).resolve(version.toString()).resolve(name + "-" + version + ".war");
    }

    private String fileSearchResult(String path) {
        return "{" //
                + "\"uri\":\"" + base("api/storage/" + path) + "\",\n" //
                + "\"downloadUri\" : \"" + base(path) + "\"\n" //
                + "}";
    }

    private boolean isIndexed(CheckSum checkSum) {
        return index().containsKey(checkSum);
    }

    private static Map<CheckSum, java.nio.file.Path> index() {
        if (INDEX.isEmpty())
            readIndex();
        return INDEX;
    }

    private static String nameFor(CheckSum checkSum) {
        return "fake-" + checkSum.hexString().substring(0, 6);
    }

    public static Version versionFor(CheckSum checkSum) {
        return versionFor(checkSum.getBytes());
    }

    private static Version versionFor(byte[] bytes) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < 3 && i <= bytes.length; i++) {
            if (i > 0)
                string.append(".");
            int digit = 0x07 & bytes[i];
            string.append(digit);
        }
        return new Version(string.toString());
    }

    @GET
    @Path("/api/storage/{repoKey}/{path:.*}")
    public Response fileOrFolderInfo(@PathParam("repoKey") String repoKey, @PathParam("path") String path)
            throws IOException {
        log.debug("get file/folder info for {} in {}", path, repoKey);
        String info = "{\n" //
                + "   \"repo\" : \"" + repoKey + "\",\n" //
                + "   \"path\" : \"" + path + "\",\n" //
                + "   \"created\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                + "   \"createdBy\" : \"kirk\",\n" //
                + "   \"modifiedBy\" : \"spock\",\n" //
                + "   \"lastModified\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                + "   \"lastUpdated\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                + info(Paths.get(path)) //
                + "   \"uri\" : \"" + base("api/storage/" + repoKey + "/" + path) + "\"\n" //
                + "}\n";
        return Response.ok(info, info.contains("\"children\"") ? FOLDER_INFO : FILE_INFO).build();
    }

    private String info(java.nio.file.Path path) throws IOException {
        if (FOO.equals(path.getName(0).toString())) {
            if (path.getNameCount() == 1)
                return folderInfo(path);
            if (path.getNameCount() == 2) {
                StringBuilder out = childrenBuilder();
                Version version = version(path.resolve("dummyFile"));
                out.append(fileChild(FOO + "-" + version + ".war"));
                out.append(fileChild(FOO + "-" + version + ".pom"));
                return closeChildrenBuilder(out);
            }
            Version version = version(path);
            return fileInfo(12345L, checksumFor(FOO, version), CheckSum.fromString("1234567890abcdef"));
        }
        java.nio.file.Path resolved = MAVEN_REPOSITORY.resolve(path);
        if (Files.isDirectory(resolved))
            return folderInfo(path);
        else if (Files.isRegularFile(resolved))
            return fileInfo(resolved);
        log.debug("fake info for: {}", path);
        CheckSum checksum = checksumFor(path.getFileName().toString());
        return fileInfo(12345, checksum, checksum);
    }

    private String folderInfo(java.nio.file.Path path) throws IOException {
        final StringBuilder out = childrenBuilder();
        if (isIndexed(path)) {
            Files.walkFileTree(MAVEN_REPOSITORY.resolve(path), EnumSet.noneOf(FileVisitOption.class), 1,
                    new SimpleFileVisitor<java.nio.file.Path>() {
                        @Override
                        public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) {
                            if (Files.isDirectory(file))
                                out.append(folderChild(file.getFileName().toString()));
                            else
                                out.append(fileChild(file.getFileName().toString()));
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } else {
            for (Version version : availableVersionsFor(path.toString())) {
                out.append(folderChild(version.toString()));
            }
        }
        return closeChildrenBuilder(out);
    }

    private StringBuilder childrenBuilder() {
        final StringBuilder out = new StringBuilder();
        out.append("   \"children\" : [\n");
        return out;
    }

    private String closeChildrenBuilder(final StringBuilder out) {
        out.setLength(out.length() - 2); // final comma and \n
        out.append("\n   ],\n");
        return out.toString();
    }

    private static boolean isIndexed(java.nio.file.Path path) {
        for (java.nio.file.Path p : index().values()) {
            if (p.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    public static List<Version> availableVersionsFor(String name) {
        switch (name.toString()) {
            case FOO:
                return FOO_VERSIONS;
            case BAR:
                return BAR_VERSIONS;
            default:
                return arbitraryVersionFor(name);
        }
    }

    private static List<Version> arbitraryVersionFor(String name) {
        byte[] bytes = name.getBytes();
        Version v = versionFor(bytes);
        bytes[1]--;
        Version m1 = versionFor(bytes);
        bytes[1]++;
        bytes[0]++;
        Version p1 = versionFor(bytes);
        bytes[1]++;
        Version p2 = versionFor(bytes);
        bytes[2]++;
        Version p3 = versionFor(bytes);
        return asList(m1, v, p1, p2, p3);
    }

    private String folderChild(String fileName) {
        return "      {\n" //
                + "         \"folder\" : true,\n" //
                + "         \"uri\" : \"/" + fileName + "\"\n" //
                + "      },\n" //
        ;
    }

    private String fileChild(String fileName) {
        return "      {\n" //
                + "         \"folder\" : false,\n" //
                + "         \"uri\" : \"/" + fileName + "\"\n" //
                + "      },\n" //
        ;
    }

    private String fileInfo(java.nio.file.Path path) throws IOException {
        long size = Files.size(path);
        CheckSum sha1 = CheckSum.sha1(path);
        CheckSum md5 = CheckSum.md5(path);
        return fileInfo(size, sha1, md5);
    }

    private String fileInfo(long size, CheckSum sha1, CheckSum md5) {
        return "  \"mimeType\" : \"application/java-archive\",\n" //
                + "  \"size\" : \"" + size + "\",\n" //
                + "  \"checksums\" : {\n" //
                + "    \"sha1\" : \"" + sha1 + "\",\n" //
                + "    \"md5\" : \"" + md5 + "\"\n" //
                + "  },\n" //
                + "  \"originalChecksums\" : {\n" //
                + "    \"sha1\" : \"" + sha1 + "\",\n" //
                + "    \"md5\" : \"" + md5 + "\"\n" //
                + "  },\n";
    }

    @GET
    @Path("/{repoKey}/{path:.*}")
    public InputStream getFile(@SuppressWarnings("unused") @PathParam("repoKey") String repoKey,
            @PathParam("path") String pathString) throws IOException {
        java.nio.file.Path path = Paths.get(pathString);
        if (isIndexed(path)) {
            java.nio.file.Path repoPath = MAVEN_REPOSITORY.resolve(path);
            log.debug("return repository file stream: {}", repoPath);
            return Files.newInputStream(repoPath);
        }
        int n = path.getNameCount();
        String name = path.getName(n - 1).toString();
        Version version = new Version(path.getName(n - 2).toString());
        return inputStreamFor(name, version);
    }

    public static InputStream inputStreamFor(String name, Version version) {
        return new StringInputStream(name + "-content@" + version);
    }

    @RequiredArgsConstructor
    private static final class BuildChecksumsFileVisitor extends SimpleFileVisitor<java.nio.file.Path> {
        private final java.nio.file.Path root;
        @Getter
        private final int count = 0;

        @Override
        public FileVisitResult visitFile(java.nio.file.Path path, BasicFileAttributes attrs) {
            if (!isDeployable(path.getFileName().toString()))
                return FileVisitResult.CONTINUE;
            CheckSum checkSum = CheckSum.sha1(path);
            java.nio.file.Path relativePath = root.relativize(path);
            // System.out.println(relativePath + " (" + count++ + ") -> " + checkSum);
            INDEX.put(checkSum, relativePath);
            return FileVisitResult.CONTINUE;
        }

        private boolean isDeployable(String fileName) {
            return fileName.endsWith(".war") || fileName.endsWith(".ear");
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("build local maven repository checksum index");
        BuildChecksumsFileVisitor visitor = new BuildChecksumsFileVisitor(MAVEN_REPOSITORY);
        Instant start = Instant.now();
        Files.walkFileTree(MAVEN_REPOSITORY, visitor);
        Instant end = Instant.now();
        Duration duration = new Duration(start, end);
        System.out.println("ended after " + PeriodFormat.getDefault().print(duration.toPeriod()) //
                + " for " + visitor.getCount() + " deployables");
        writeIndex();
    }

    @SneakyThrows(IOException.class)
    public static void readIndex() {
        try (BufferedReader reader = Files.newBufferedReader(MAVEN_INDEX_FILE, UTF_8)) {
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                int index = line.indexOf(":");
                if (index != 40)
                    throw new IllegalStateException("unexpected line in index file");
                CheckSum checkSum = CheckSum.ofHexString(line.substring(0, index));
                java.nio.file.Path path = Paths.get(line.substring(index + 1));
                INDEX.put(checkSum, path);
            }
        }
    }

    @SneakyThrows(IOException.class)
    public static void writeIndex() {
        try (BufferedWriter writer = Files.newBufferedWriter(MAVEN_INDEX_FILE, UTF_8)) {
            for (Map.Entry<CheckSum, java.nio.file.Path> entry : INDEX.entrySet()) {
                writer.append(entry.getKey().hexString()).append(":") //
                        .append(entry.getValue().toString()).append("\n");
            }
        }
    }
}
