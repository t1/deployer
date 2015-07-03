package com.github.t1.deployer.repository;

import static com.github.t1.deployer.repository.ArtifactoryRepository.*;
import static com.github.t1.deployer.tools.StatusDetails.*;
import static java.util.Arrays.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import com.github.t1.deployer.model.*;

/**
 * @see ArtifactoryMockLauncher
 * @see ArtifactoryMockIndexBuilder
 */
@Slf4j
@Path("/artifactory")
public class ArtifactoryMock {
    public static boolean FAKES = false;

    private static final MediaType FILE_INFO = vendorTypeJFrog("FileInfo");
    private static final MediaType FOLDER_INFO = vendorTypeJFrog("FolderInfo");

    private static MediaType vendorTypeJFrog(String type) {
        return MediaType.valueOf("application/vnd.org.jfrog.artifactory.storage." + type + "+json");
    }

    static final Charset UTF_8 = Charset.forName("UTF-8");

    static final java.nio.file.Path MAVEN_HOME = Paths.get(System.getProperty("user.home"), ".m2");
    static final java.nio.file.Path MAVEN_REPOSITORY = MAVEN_HOME.resolve("repository");
    static final java.nio.file.Path MAVEN_INDEX_FILE = MAVEN_HOME.resolve("checksum.index");

    private static final Map<CheckSum, java.nio.file.Path> INDEX = new HashMap<>();

    static Map<CheckSum, java.nio.file.Path> index() {
        if (INDEX.isEmpty())
            readIndex();
        return INDEX;
    }

    @SneakyThrows(IOException.class)
    private static void readIndex() {
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

    private static final java.nio.file.Path REPO_NAME = Paths.get("libs-release-local");

    public static final CheckSum FAILING_CHECKSUM = CheckSum.ofHexString("1111111111111111111111111111111111111111");
    public static final CheckSum AMBIGUOUS_CHECKSUM = CheckSum.ofHexString("2222222222222222222222222222222222222222");
    public static final CheckSum UNKNOWN_CHECKSUM = CheckSum.ofHexString("3333333333333333333333333333333333333333");

    public static final ContextRoot FOO = new ContextRoot("foo");
    public static final ContextRoot BAR = new ContextRoot("bar");

    public static final DeploymentName FOO_WAR = new DeploymentName(FOO + ".war");
    public static final DeploymentName BAR_WAR = new DeploymentName(BAR + ".war");

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
        log.info("search by checksum: {}", checkSum);
        if (checkSum == null)
            throw badRequest("Required query parameter 'sha1' is missing.");
        return "{\"results\": [" + searchResultsFor(checkSum) + "]}";
    }

    private String searchResultsFor(CheckSum checkSum) {
        if (fakeChecksumFor(FOO).equals(checkSum)) {
            return fileSearchResult(FOO, CURRENT_FOO_VERSION);
        } else if (fakeChecksumFor(BAR).equals(checkSum)) {
            return fileSearchResult(BAR, CURRENT_BAR_VERSION);
        } else if (FAILING_CHECKSUM.equals(checkSum)) {
            throw new RuntimeException("fake error in repo");
        } else if (AMBIGUOUS_CHECKSUM.equals(checkSum)) {
            return fileSearchResult(new ContextRoot("x"), new Version("1.0")) + ","
                    + fileSearchResult(new ContextRoot("y"), new Version("2.0"));
        } else if (UNKNOWN_CHECKSUM.equals(checkSum)) {
            return "";
        } else if (isIndexed(checkSum)) {
            java.nio.file.Path path = index().get(checkSum);
            return fileSearchResult(REPO_NAME + "/" + path);
        } else if (FAKES) {
            ContextRoot contextRoot = fakeContextRootFor(checkSum);
            Version version = fakeVersionFor(checkSum);
            log.info("fake search result for {}: {}@{}", checkSum, contextRoot, version);
            return fileSearchResult(contextRoot, version);
        } else {
            return "";
        }
    }

    public static CheckSum fakeChecksumFor(ContextRoot contextRoot) {
        return fakeChecksumFor(contextRoot, fakeVersionFor(contextRoot));
    }

    public static Version fakeVersionFor(ContextRoot contextRoot) {
        if (FOO.equals(contextRoot)) {
            return CURRENT_FOO_VERSION;
        } else if (BAR.equals(contextRoot)) {
            return CURRENT_BAR_VERSION;
        } else {
            return fakeVersionFor(contextRoot.getValue().getBytes());
        }
    }

    public static CheckSum fakeChecksumFor(ContextRoot name, Version version) {
        CheckSum result = CheckSum.sha1((name + "@" + version).getBytes()); // arbitrary but fixed for name/version
        result.getBytes()[0] = (byte) 0xFA;
        result.getBytes()[1] = (byte) 0xCE;
        result.getBytes()[2] = (byte) 0x00;
        result.getBytes()[3] = (byte) 0x00;
        return result;
    }

    private String fileSearchResult(ContextRoot contextRoot, Version version) {
        return fileSearchResult(pathFor(contextRoot, version).toString());
    }

    public static java.nio.file.Path pathFor(ContextRoot contextRoot, Version version) {
        return REPO_NAME.resolve(contextRoot.toString()).resolve(version.toString())
                .resolve(contextRoot + "-" + version + ".war");
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

    private static ContextRoot fakeContextRootFor(CheckSum checkSum) {
        if (checkSum.hexString().length() < 6)
            throw badRequest("checkSum too short. must be at least 6 characters for fake context root: ["
                    + checkSum.hexString() + "]");
        return new ContextRoot("fake-" + checkSum.hexString().substring(0, 6));
    }

    public static Version fakeVersionFor(CheckSum checkSum) {
        return fakeVersionFor(checkSum.getBytes());
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
    public Response fileOrFolderInfo(@PathParam("repoKey") String repoKey, @PathParam("path") String path)
            throws IOException {
        log.info("get file/folder info for {} in {}", path, repoKey);
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
        if (FOO.getValue().equals(path.getName(0).toString())) {
            log.info("foo info {}", path);
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
            log.info("foo info for {}", version);
            return fileInfo(12345L, fakeChecksumFor(FOO, version), CheckSum.fromString("1234567890abcdef"));
        }
        java.nio.file.Path resolved = MAVEN_REPOSITORY.resolve(path);
        log.info("info from {}", resolved);
        if (Files.isDirectory(resolved))
            return folderInfo(path);
        else if (Files.isRegularFile(resolved))
            return fileInfo(resolved);
        if (FAKES) {
            log.info("fake file info for: {}", path);
            CheckSum checksum = fakeChecksumFor(new ContextRoot(path.getFileName().toString()));
            return fileInfo(12345, checksum, checksum);
        }
        throw new WebApplicationException(Response.status(NOT_FOUND).type(APPLICATION_JSON).entity("{\n" //
                + "  \"errors\" : [ {\n" //
                + "    \"status\" : 404,\n" //
                + "    \"message\" : \"Unable to find item\"\n" //
                + "  } ]\n" //
                + "}").build());
    }

    private String folderInfo(java.nio.file.Path path) throws IOException {
        final StringBuilder out = childrenBuilder();
        if (isIndexed(path)) {
            log.info("indexed folder info {}", path);
            Files.walkFileTree(MAVEN_REPOSITORY.resolve(path), EnumSet.noneOf(FileVisitOption.class), 1,
                    new SimpleFileVisitor<java.nio.file.Path>() {
                        @Override
                        public FileVisitResult visitFile(java.nio.file.Path path, BasicFileAttributes attrs) {
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
            for (Version version : fakeVersionsFor(new ContextRoot(path.toString()))) {
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

    public static List<Version> fakeVersionsFor(ContextRoot contextRoot) {
        if (contextRoot.equals(FOO)) {
            return FOO_VERSIONS;
        } else if (contextRoot.equals(BAR)) {
            return BAR_VERSIONS;
        } else {
            return fakeVersionsFor(contextRoot.getValue().getBytes());
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
        log.info("file info {}: size={}, sha1={}, md5={}", path, size, sha1, md5);
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
    @Produces("application/java-archive")
    public InputStream getFileStream(@SuppressWarnings("unused") @PathParam("repoKey") String repoKey,
            @PathParam("path") String pathString) throws IOException {
        java.nio.file.Path path = Paths.get(pathString);
        if (isIndexed(path)) {
            java.nio.file.Path repoPath = MAVEN_REPOSITORY.resolve(path);
            log.info("return repository file stream: {}", repoPath);
            return Files.newInputStream(repoPath);
        }
        int n = path.getNameCount();
        ContextRoot contextRoot = new ContextRoot(path.getName(n - 1).toString());
        Version version = new Version(path.getName(n - 2).toString());
        return inputStreamFor(contextRoot, version);
    }

    public static InputStream inputStreamFor(ContextRoot contextRoot, Version version) {
        return new StringInputStream(contextRoot + "@" + version);
    }
}
