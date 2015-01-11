package com.github.t1.deployer;

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

import org.joda.time.*;
import org.joda.time.format.PeriodFormat;

/** If you don't have a real Artifactory Pro available, move this to main and configure the endpoint. */
@Path("/artifactory")
public class ArtifactoryMock {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

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
        return "{\"results\": [" + searchResultsFor(checkSum) + "]}";
    }

    private String searchResultsFor(CheckSum checkSum) {
        if (checksumFor(FOO).equals(checkSum)) {
            return uriWar(FOO, CURRENT_FOO_VERSION);
        } else if (checksumFor(BAR).equals(checkSum)) {
            return uriWar(BAR, CURRENT_BAR_VERSION);
        } else if (FAILING_CHECKSUM.equals(checkSum)) {
            throw new RuntimeException("error in repo");
        } else if (AMBIGUOUS_CHECKSUM.equals(checkSum)) {
            return uriWar("x", null) + "," + uriWar("y", null);
        } else if (UNKNOWN_CHECKSUM.equals(checkSum)) {
            return "";
        } else if (isIndexed(checkSum)) {
            java.nio.file.Path path = index().get(checkSum);
            return uriWar("/repo/" + path);
        } else {
            String name = nameFor(checkSum);
            Version version = versionFor(checkSum);
            return uriWar(name, version);
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
        return CheckSum.of(("checkSum(" + name + "@" + version + ")").getBytes());
    }

    private String uriWar(String name, Version version) {
        return uriWar("/libs-release-local/" + name + "/" + version + "/" + name + "-" + version + ".war");
    }

    private String uriWar(String path) {
        return "{" //
                + "\"uri\":\"" + base("api/storage" + path) + "\",\n" //
                + "\"downloadUri\" : \"" + base(path) + "\"\n" //
                + "}";
    }

    private boolean isIndexed(CheckSum checkSum) {
        return index().containsKey(checkSum);
    }

    private static Version version(java.nio.file.Path path) {
        return new Version(path.getName(path.getNameCount() - 2).toString());
    }

    private static Map<CheckSum, java.nio.file.Path> index() {
        if (INDEX.isEmpty())
            readIndex();
        return INDEX;
    }

    private static String nameFor(CheckSum checkSum) {
        return checkSum.hexString().substring(0, 6);
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
    @Path("/api/storage/{repoKey}/{folder-path:.*}")
    @Produces("application/vnd.org.jfrog.artifactory.storage.FolderInfo+json")
    public String listFiles(@PathParam("repoKey") String repoKey, @PathParam("folder-path") String folderPath) {
        return "{\n" //
                + "   \"path\" : \"" + folderPath + "\",\n" //
                + "   \"createdBy\" : \"kirk\",\n" //
                + "   \"repo\" : \"" + repoKey + "\",\n" //
                + "   \"modifiedBy\" : \"spock\",\n" //
                + "   \"created\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                + "   \"lastUpdated\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                + "   \"children\" : [\n" //
                + foldersIn(folderPath) //
                + "      {\n" //
                + "         \"folder\" : false,\n" //
                + "         \"uri\" : \"/maven-metadata.xml\"\n" //
                + "      }\n" //
                + "   ],\n" //
                + "   \"lastModified\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                + "   \"uri\" : \"" + base(repoKey + "/" + folderPath) + "\"\n" //
                + "}\n" //
        ;
    }

    private String foldersIn(String path) {
        StringBuilder out = new StringBuilder();
        for (Version version : availableVersionsFor(path)) {
            out.append(version(version.getVersion()));
        }
        return out.toString();
    }

    public static List<Version> availableVersionsFor(String path) {
        if (isIndexed(Paths.get(path))) {
            List<Version> result = new ArrayList<>();
            for (java.nio.file.Path p : index().values()) {
                if (p.startsWith(path)) {
                    result.add(version(p));
                }
            }
            return result;
        }
        switch (path) {
            case FOO:
                return FOO_VERSIONS;
            case BAR:
                return BAR_VERSIONS;
            default:
                return arbitraryVersionFor(path);
        }
    }

    private static boolean isIndexed(java.nio.file.Path path) {
        for (java.nio.file.Path p : index().values()) {
            if (p.startsWith(path)) {
                return true;
            }
        }
        return false;
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

    private String version(String version) {
        return "      {\n" //
                + "         \"folder\" : true,\n" //
                + "         \"uri\" : \"/" + version + "\"\n" //
                + "      },\n" //
        ;
    }

    @GET
    @Path("/{repoKey}/{path:.*}")
    public InputStream getFile(@SuppressWarnings("unused") @PathParam("repoKey") String repoKey,
            @PathParam("path") String pathString) throws IOException {
        java.nio.file.Path path = Paths.get(pathString);
        if (isIndexed(path)) {
            java.nio.file.Path repoPath = MAVEN_REPOSITORY.resolve(path);
            System.out.println("return repository file stream: " + repoPath);
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
            CheckSum checkSum = CheckSum.of(path);
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
