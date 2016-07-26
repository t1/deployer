package com.github.t1.deployer.repository;

import com.github.t1.deployer.container.DeploymentName;
import com.github.t1.deployer.model.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static com.github.t1.deployer.repository.ArtifactoryRepository.*;
import static java.util.Arrays.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * @see ArtifactoryMockLauncher
 * @see ArtifactoryMockIndexBuilder
 */
@Slf4j
@Path("/artifactory")
public class ArtifactoryMock {
    private static final String BASIC_FOO_BAR_AUTHORIZATION = "Basic Zm9vOmJhcg==";

    static boolean FAKES = false;

    private static final MediaType FILE_INFO = vendorTypeJFrog("FileInfo");
    private static final MediaType FOLDER_INFO = vendorTypeJFrog("FolderInfo");

    private static MediaType vendorTypeJFrog(String type) {
        return MediaType.valueOf("application/vnd.org.jfrog.artifactory.storage." + type + "+json");
    }

    static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final java.nio.file.Path MAVEN_HOME = Paths.get(System.getProperty("user.home"), ".m2");
    static final java.nio.file.Path MAVEN_REPOSITORY = MAVEN_HOME.resolve("repository");
    static final java.nio.file.Path MAVEN_INDEX_FILE = MAVEN_HOME.resolve("checksum.index");

    static final Map<Checksum, java.nio.file.Path> INDEX = new HashMap<>();

    static Map<Checksum, java.nio.file.Path> index() {
        if (INDEX.isEmpty())
            readIndex();
        return INDEX;
    }

    @SneakyThrows(IOException.class)
    private static void readIndex() {
        if (Files.isReadable(MAVEN_INDEX_FILE))
            try (BufferedReader reader = Files.newBufferedReader(MAVEN_INDEX_FILE, UTF_8)) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null)
                        break;
                    int index = line.indexOf(":");
                    if (index != 40)
                        throw new IllegalStateException("unexpected line in index file");
                    Checksum checksum = Checksum.ofHexString(line.substring(0, index));
                    java.nio.file.Path path = Paths.get(line.substring(index + 1));
                    INDEX.put(checksum, path);
                }
            }
    }

    private static final java.nio.file.Path REPO_NAME = Paths.get("libs-release-local");

    static final Checksum FAILING_CHECKSUM = Checksum.ofHexString("1111111111111111111111111111111111111111");
    static final Checksum AMBIGUOUS_CHECKSUM = Checksum.ofHexString("2222222222222222222222222222222222222222");
    static final Checksum UNKNOWN_CHECKSUM = Checksum.ofHexString("3333333333333333333333333333333333333333");

    public static final DeploymentName FOO = new DeploymentName("foo");
    public static final DeploymentName BAR = new DeploymentName("bar");

    private static final Version NEWEST_FOO_VERSION = new Version("1.3.10");
    static final Version CURRENT_FOO_VERSION = new Version("1.3.1");

    static final List<Version> FOO_VERSIONS = asList(//
            NEWEST_FOO_VERSION,
            new Version("1.3.12"),
            new Version("1.3.2"),
            CURRENT_FOO_VERSION,
            new Version("1.3.0"),
            new Version("1.2.1"),
            new Version("1.2.1.1"),
            new Version("1.2.1-SNAPSHOT"),
            new Version("1.2.0")
    );

    private static final Version CURRENT_BAR_VERSION = new Version("0.3");

    private static final List<Version> BAR_VERSIONS = asList(
            CURRENT_BAR_VERSION,
            new Version("0.2")
    );

    public static final class StringInputStream extends ByteArrayInputStream {
        private final String string;

        public StringInputStream(String string) {
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

    private URI base(String path) { return uriInfo.getBaseUriBuilder().path(path).build(); }

    @Setter
    private boolean requireAuthorization = false;

    @GET
    @Path("/api/search/checksum")
    @Produces("application/vnd.org.jfrog.artifactory.search.ChecksumSearchResult+json")
    public String searchByChecksum(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("sha1") Checksum checksum) {
        checkAuthorization(authorization);
        log.info("search by checksum: {}", checksum);
        if (checksum == null)
            throw new RuntimeException("Required query parameter 'sha1' is missing.");
        String results = searchResultsFor(checksum);
        log.info("found {}", results);
        return "{\"results\": [" + results + "]}";
    }

    private void checkAuthorization(String authorization) {
        if (!requireAuthorization)
            return;
        if (authorization == null)
            throw new RuntimeException("missing authorization");
        if (!BASIC_FOO_BAR_AUTHORIZATION.equals(authorization))
            throw new RuntimeException("wrong credentials");
    }

    private String searchResultsFor(Checksum checksum) {
        if (fakeChecksumFor(FOO).equals(checksum)) {
            return fileSearchResult(FOO, CURRENT_FOO_VERSION);
        } else if (fakeChecksumFor(BAR).equals(checksum)) {
            return fileSearchResult(BAR, CURRENT_BAR_VERSION);
        } else if (FAILING_CHECKSUM.equals(checksum)) {
            throw new RuntimeException("fake error in repo");
        } else if (AMBIGUOUS_CHECKSUM.equals(checksum)) {
            return fileSearchResult(new DeploymentName("x"), new Version("1.0")) + ","
                    + fileSearchResult(new DeploymentName("y"), new Version("2.0"));
        } else if (UNKNOWN_CHECKSUM.equals(checksum)) {
            return "";
        } else if (isIndexed(checksum)) {
            java.nio.file.Path path = index().get(checksum);
            return fileSearchResult(REPO_NAME + "/" + path);
        } else if (FAKES) {
            DeploymentName name = fakeNameFor(checksum);
            Version version = fakeVersionFor(checksum);
            log.info("fake search result for {}: {}@{}", checksum, name, version);
            return fileSearchResult(name, version);
        } else {
            return "";
        }
    }

    public static Checksum fakeChecksumFor(DeploymentName name) {
        return fakeChecksumFor(name, fakeVersionFor(name));
    }

    public static Version fakeVersionFor(DeploymentName name) {
        if (FOO.equals(name)) {
            return CURRENT_FOO_VERSION;
        } else if (BAR.equals(name)) {
            return CURRENT_BAR_VERSION;
        } else {
            return fakeVersionFor(name.getValue().getBytes());
        }
    }

    public static Checksum fakeChecksumFor(String name, String version) {
        return fakeChecksumFor(new DeploymentName(name), new Version(version));
    }

    public static Checksum fakeChecksumFor(DeploymentName name, Version version) {
        Checksum result = Checksum.sha1((name + "@" + version).getBytes()); // arbitrary but fixed for name/version
        result.getBytes()[0] = (byte) 0xFA;
        result.getBytes()[1] = (byte) 0xCE;
        result.getBytes()[2] = (byte) 0x00;
        result.getBytes()[3] = (byte) 0x00;
        return result;
    }

    private String fileSearchResult(DeploymentName name, Version version) {
        return fileSearchResult(fakePathFor(name, version).toString());
    }

    private static java.nio.file.Path fakePathFor(DeploymentName name, Version version) {
        return REPO_NAME.resolve(fakeGroupId(name).replace(".", "/"))
                        .resolve(fakeArtifactId(name))
                        .resolve(version.toString())
                        .resolve(name + "-" + version + ".war");
    }

    @NotNull private static String fakeGroupId(DeploymentName name) {return "org." + name;}

    @NotNull private static String fakeArtifactId(DeploymentName name) {return name + "-war";}

    private String fileSearchResult(String path) {
        return "{"
                + "\"uri\":\"" + base("api/storage/" + path) + "\",\n"
                + "\"downloadUri\" : \"" + base(path) + "\"\n"
                + "}";
    }

    private boolean isIndexed(Checksum checksum) {
        return index().containsKey(checksum);
    }

    private static DeploymentName fakeNameFor(Checksum checksum) {
        if (checksum.hexString().length() < 6)
            throw new RuntimeException("checkSum too short. must be at least 6 characters for fake context root: ["
                    + checksum.hexString() + "]");
        return new DeploymentName("fake-" + checksum.hexString().substring(0, 6));
    }

    private static Version fakeVersionFor(Checksum checksum) {
        return fakeVersionFor(checksum.getBytes());
    }

    private static Version fakeVersionFor(byte[] bytes) {
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
    public Response fileOrFolderInfo(@HeaderParam("Authorization") String authorization,
            @PathParam("repoKey") String repoKey, @PathParam("path") String path) throws IOException {
        checkAuthorization(authorization);
        log.info("get file/folder info for {} in {}", path, repoKey);
        String info = "{\n"
                + "   \"repo\" : \"" + repoKey + "\",\n"
                + "   \"path\" : \"/" + path + "\",\n"
                + "   \"created\" : \"2014-04-02T16:21:31.385+02:00\",\n"
                + "   \"createdBy\" : \"kirk\",\n"
                + "   \"modifiedBy\" : \"spock\",\n"
                + "   \"lastModified\" : \"2014-04-02T16:21:31.385+02:00\",\n"
                + "   \"lastUpdated\" : \"2014-04-02T16:21:31.385+02:00\",\n"
                + info(Paths.get(path))
                + "   \"downloadUri\": \"http://localhost:8081/artifactory/" + repoKey + "/" + path + "\",\n"
                + "   \"remoteUrl\": \"http://jcenter.bintray.com/" + path + "\",\n"
                + "   \"uri\" : \"" + base("api/storage/" + repoKey + "/" + path) + "\"\n"
                + "}\n";
        return Response.ok(info, info.contains("\"children\"") ? FOLDER_INFO : FILE_INFO).build();
    }

    private String info(java.nio.file.Path path) throws IOException {
        if (FOO.getValue().equals(path.getName(0).toString())) {
            log.info("foo info {}", path);
            if (path.getNameCount() == 1)
                return folderInfo(path);
            if (path.getNameCount() == 2) {
                StringBuilder out = childrenBuilder();
                Version version = versionFrom(path.resolve("dummyFile"));
                out.append(fileChild(FOO + "-" + version + ".war"));
                out.append(fileChild(FOO + "-" + version + ".pom"));
                return closeChildrenBuilder(out);
            }
            Version version = versionFrom(path);
            log.info("foo info for {}", version);
            return fileInfo(12345L, fakeChecksumFor(FOO, version), Checksum.fromString("1234567890abcdef"));
        }
        java.nio.file.Path resolved = MAVEN_REPOSITORY.resolve(path);
        log.info("info from {}", resolved);
        if (Files.isDirectory(resolved))
            return folderInfo(path);
        else if (Files.isRegularFile(resolved))
            return fileInfo(resolved);
        if (FAKES) {
            log.info("fake file info for: {}", path);
            Checksum checksum = fakeChecksumFor(new DeploymentName(path.getFileName().toString()));
            return fileInfo(12345, checksum, checksum);
        }
        throw new WebApplicationException(Response
                .status(NOT_FOUND)
                .type(APPLICATION_JSON)
                .entity("{\n"
                        + "  \"errors\" : [ {\n"
                        + "    \"status\" : 404,\n"
                        + "    \"message\" : \"Unable to find item\"\n"
                        + "  } ]\n"
                        + "}")
                .build());
    }

    private String folderInfo(java.nio.file.Path path) throws IOException {
        final StringBuilder out = childrenBuilder();
        if (isIndexed(path)) {
            log.info("indexed folder info {}", path);
            Files.walkFileTree(MAVEN_REPOSITORY.resolve(path), EnumSet.noneOf(FileVisitOption.class), 1,
                    new SimpleFileVisitor<java.nio.file.Path>() {
                        @Override
                        public FileVisitResult visitFile(java.nio.file.Path path, BasicFileAttributes attributes) {
                            String fileName = path.getFileName().toString();
                            if (Files.isDirectory(path)) {
                                String folder = folderChild(fileName);
                                out.append(folder);
                            } else {
                                String file = fileChild(fileName);
                                out.append(file);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } else if (FAKES) {
            log.info("fake folder info {}", path);
            for (Version version : fakeVersionsFor(new DeploymentName(path.toString()))) {
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

    public static List<Version> fakeVersionsFor(DeploymentName name) {
        if (name.equals(FOO)) {
            return FOO_VERSIONS;
        } else if (name.equals(BAR)) {
            return BAR_VERSIONS;
        } else {
            return fakeVersionsFor(name.getValue().getBytes());
        }
    }

    private static List<Version> fakeVersionsFor(byte[] bytes) {
        Version v = fakeVersionFor(bytes);
        bytes[1]--;
        Version m1 = fakeVersionFor(bytes);
        bytes[1]++;
        bytes[0]++;
        Version p1 = fakeVersionFor(bytes);
        bytes[1]++;
        Version p2 = fakeVersionFor(bytes);
        bytes[2]++;
        Version p3 = fakeVersionFor(bytes);
        return asList(m1, v, p1, p2, p3);
    }

    private String folderChild(String fileName) {
        return "      {\n"
                + "         \"folder\" : true,\n"
                + "         \"uri\" : \"/" + fileName + "\"\n"
                + "      },\n"
                ;
    }

    private String fileChild(String fileName) {
        return "      {\n"
                + "         \"folder\" : false,\n"
                + "         \"uri\" : \"/" + fileName + "\"\n"
                + "      },\n"
                ;
    }

    private String fileInfo(java.nio.file.Path path) throws IOException {
        long size = Files.size(path);
        Checksum sha1 = Checksum.sha1(path);
        Checksum md5 = Checksum.md5(path);
        log.info("file info {}: size={}, sha1={}, md5={}", path, size, sha1, md5);
        return fileInfo(size, sha1, md5);
    }

    private String fileInfo(long size, Checksum sha1, Checksum md5) {
        return "  \"mimeType\" : \"application/java-archive\",\n"
                + "  \"size\" : \"" + size + "\",\n"
                + "  \"checksums\" : {\n"
                + "    \"sha1\" : \"" + sha1 + "\",\n"
                + "    \"md5\" : \"" + md5 + "\"\n"
                + "  },\n"
                + "  \"originalChecksums\" : {\n"
                + "    \"sha1\" : \"" + sha1 + "\"\n"
                // + "    \"md5\" : \"" + md5 + "\"\n"
                + "  },\n";
    }

    @GET
    @Path("/{repoKey}/{path:.*}")
    @Produces("application/java-archive")
    public InputStream getFileStream(@HeaderParam("Authorization") String authorization,
            @SuppressWarnings("unused") @PathParam("repoKey") String repoKey, @PathParam("path") String pathString)
            throws IOException {
        checkAuthorization(authorization);
        java.nio.file.Path path = Paths.get(pathString);
        if (isIndexed(path)) {
            java.nio.file.Path repoPath = MAVEN_REPOSITORY.resolve(path);
            log.info("return repository file stream: {}", repoPath);
            return Files.newInputStream(repoPath);
        }
        int n = path.getNameCount();
        DeploymentName name = new DeploymentName(path.getName(n - 1).toString());
        Version version = new Version(path.getName(n - 2).toString());
        return inputStreamFor(name, version);
    }

    public static InputStream inputStreamFor(DeploymentName name, Version version) {
        return new StringInputStream(name + "@" + version);
    }
}
