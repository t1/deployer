package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.testtools.WebArchiveBuilder;
import com.google.common.collect.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.format.*;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryRepository.*;
import static com.github.t1.problem.WebException.*;
import static java.lang.ProcessBuilder.Redirect.*;
import static java.time.ZoneOffset.*;
import static java.util.Arrays.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * @see ArtifactoryMockLauncher
 * @see ArtifactoryMockIndexBuilder
 */
@Slf4j
@Path("/")
public class ArtifactoryMock {
    @SuppressWarnings("SpellCheckingInspection") public static final DateTimeFormatter TIMESTAMP
            = new DateTimeFormatterBuilder().appendPattern("yyyyMMddHHmmss").toFormatter();
    private static final String BASIC_FOO_BAR_AUTHORIZATION = "Basic Zm9vOmJhcg==";
    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

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

    static final BiMap<Checksum, java.nio.file.Path> INDEX = HashBiMap.create();

    public static Checksum dummyWar(GroupId groupId, ArtifactId artifactId, Version version) {
        java.nio.file.Path path = toPath(groupId, artifactId, war, version);
        if (!isIndexed(path)) {
            java.nio.file.Path fullPath = MAVEN_REPOSITORY.resolve(path);
            createDummyWar(artifactId.getValue(), fullPath);
            createDummyMetaData(groupId, artifactId, version, fullPath);
            index(path);
        }
        return Objects.requireNonNull(index().inverse().get(path),
                "no war checksum created for " + groupId + ":" + artifactId + ":" + version);
    }

    @SneakyThrows(IOException.class)
    private static void createDummyWar(String name, java.nio.file.Path path) {
        log.debug("create dummy war {} in {}", name, path);
        WebArchive webArchive = new WebArchiveBuilder(name).with(String.class).print().build();
        Files.createDirectories(path.getParent());
        new ZipExporterImpl(webArchive).exportTo(Files.newOutputStream(path));
    }

    @SneakyThrows(IOException.class)
    private static void createDummyMetaData(GroupId groupId, ArtifactId artifactId, Version version,
            java.nio.file.Path path) {
        log.debug("create dummy meta-data {}:{}:{} in {}", groupId, artifactId, version, path);
        String lastUpdate = TIMESTAMP.format(Files.getLastModifiedTime(path).toInstant().atOffset(UTC));
        Files.write(path.getParent().resolve("maven-metadata-local.xml"),
                ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                         + "<metadata modelVersion=\"1.1.0\">\n"
                         + "  <groupId>" + groupId + "</groupId>\n"
                         + "  <artifactId>" + artifactId + "</artifactId>\n"
                         + "  <version>" + version + "</version>\n"
                         + "  <versioning>\n"
                         + "    <snapshot>\n"
                         + "      <localCopy>true</localCopy>\n"
                         + "    </snapshot>\n"
                         + "    <lastUpdated>" + lastUpdate + "</lastUpdated>\n"
                         + "    <snapshotVersions>\n"
                         + "      <snapshotVersion>\n"
                         + "        <extension>war</extension>\n"
                         + "        <value>" + version + "</value>\n"
                         + "        <updated>" + lastUpdate + "</updated>\n"
                         + "      </snapshotVersion>\n"
                         + "      <snapshotVersion>\n"
                         + "        <extension>pom</extension>\n"
                         + "        <value>" + version + "</value>\n"
                         + "        <updated>" + lastUpdate + "</updated>\n"
                         + "      </snapshotVersion>\n"
                         + "    </snapshotVersions>\n"
                         + "  </versioning>\n"
                         + "</metadata>\n"
                ).getBytes());
    }

    public static Checksum checksumFor(GroupId groupId, ArtifactId artifactId, ArtifactType type, Version version) {
        java.nio.file.Path path = toPath(groupId, artifactId, type, version);
        if (!isIndexed(path)) {
            download(groupId, artifactId, type, version);
            index(path);
        }
        return Objects.requireNonNull(index().inverse().get(path),
                "no checksum for " + groupId + ":" + artifactId + ":" + type + ":" + version);
    }

    private static java.nio.file.Path toPath(GroupId groupId, ArtifactId artifactId, ArtifactType type,
            Version version) {
        return groupId.asPath().resolve(artifactId.getValue()).resolve(version.getValue())
                      .resolve(artifactId + "-" + version + "." + type);
    }

    @SneakyThrows({ IOException.class, InterruptedException.class })
    private static void download(GroupId groupId, ArtifactId artifactId, ArtifactType type, Version version) {
        log.debug("download {}:{}:{}:{}", groupId, artifactId, type, version);
        File out = Paths.get("target/download-" + groupId + ":" + artifactId + ":" + type + ":" + version + ".out")
                        .toFile();
        Process process = new ProcessBuilder(
                "mvn",
                "dependency:get",
                "-D" + "groupId=" + groupId,
                "-D" + "artifactId=" + artifactId,
                "-D" + "packaging=" + type,
                "-D" + "version=" + version)
                .redirectOutput(appendTo(out))
                .redirectError(appendTo(out))
                .start();
        int returnCode = process.waitFor();
        assert returnCode == 0 : "unexpected return code: " + returnCode + ". see mvn output in " + out;
    }

    private static void index(java.nio.file.Path path) {
        Checksum checksum = Checksum.sha1(MAVEN_REPOSITORY.resolve(path));
        INDEX.put(checksum, path);
        writeIndex();
    }

    static BiMap<Checksum, java.nio.file.Path> index() {
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

    @SneakyThrows(IOException.class)
    static void writeIndex() {
        try (BufferedWriter writer = Files.newBufferedWriter(MAVEN_INDEX_FILE, UTF_8)) {
            INDEX.entrySet().stream()
                 .sorted(Comparator.comparing(Map.Entry::getValue))
                 .forEach(entry -> write(writer, entry));
        }
    }

    private static void write(BufferedWriter writer, Map.Entry<Checksum, java.nio.file.Path> entry) {
        try {
            writer.append(entry.getKey().hexString()).append(":")
                  .append(entry.getValue().toString()).append("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    static final List<Version> FOO_VERSIONS = asList(
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

    private static String fakeGroupId(DeploymentName name) { return "org." + name; }

    private static String fakeArtifactId(DeploymentName name) { return name + "-war"; }

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
                + info(Paths.get(path), repoKey.toLowerCase().contains("snapshot"))
                + "   \"downloadUri\": \"" + base(repoKey + "/" + path) + "\",\n"
                + "   \"remoteUrl\": \"http://jcenter.bintray.com/" + path + "\",\n"
                + "   \"uri\" : \"" + base("api/storage/" + repoKey + "/" + path) + "\"\n"
                + "}\n";
        return Response.ok(info, info.contains("\"children\"") ? FOLDER_INFO : FILE_INFO).build();
    }

    private String info(java.nio.file.Path path, boolean snapshot) throws IOException {
        if (FOO.getValue().equals(path.getName(0).toString())) {
            log.info("foo info {}", path);
            if (path.getNameCount() == 1)
                return folderInfo(path, snapshot);
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
            return folderInfo(path, snapshot);
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

    private String folderInfo(java.nio.file.Path path, boolean snapshot) throws IOException {
        final StringBuilder out = childrenBuilder();
        if (isIndexed(path)) {
            log.info("indexed folder info {}", path);
            Files.walkFileTree(MAVEN_REPOSITORY.resolve(path), EnumSet.noneOf(FileVisitOption.class), 1,
                    new SimpleFileVisitor<java.nio.file.Path>() {
                        @Override
                        public FileVisitResult visitFile(java.nio.file.Path path, BasicFileAttributes attributes) {
                            String fileName = path.getFileName().toString();
                            if (Files.isDirectory(path)) {
                                boolean snapshotFolder = fileName.contains("-SNAPSHOT");
                                if (snapshot == snapshotFolder) {
                                    String folder = folderChild(fileName);
                                    out.append(folder);
                                }
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
        for (java.nio.file.Path p : index().values())
            if (p.startsWith(path) && Files.exists(MAVEN_REPOSITORY.resolve(path)))
                return true;
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
    @Produces(APPLICATION_XML)
    public InputStream getMetaData(
            @HeaderParam("Authorization") String authorization,
            @SuppressWarnings("unused") @PathParam("repoKey") String repoKey,
            @PathParam("path") String pathString)
            throws IOException {
        checkAuthorization(authorization);
        if (!pathString.endsWith(MAVEN_METADATA_XML))
            throw notFound("mock can only serve xml for " + MAVEN_METADATA_XML);
        pathString = pathString.substring(0, pathString.length() - MAVEN_METADATA_XML.length())
                + "maven-metadata-local.xml";
        java.nio.file.Path path = Paths.get(pathString);
        java.nio.file.Path repoPath = MAVEN_REPOSITORY.resolve(path);
        if (!Files.isRegularFile(repoPath))
            throw notFound("not found " + repoPath);
        log.info("return repository file stream: {}", repoPath);
        return Files.newInputStream(repoPath);
    }

    @GET
    @Path("/{repoKey}/{path:.*}")
    @Produces("application/java-archive")
    public InputStream getFileStream(
            @HeaderParam("Authorization") String authorization,
            @SuppressWarnings("unused") @PathParam("repoKey") String repoKey,
            @PathParam("path") String pathString)
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
