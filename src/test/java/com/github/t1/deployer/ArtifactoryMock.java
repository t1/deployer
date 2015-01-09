package com.github.t1.deployer;

import static java.util.Arrays.*;

import java.io.*;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/artifactory")
public class ArtifactoryMock {
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
    @Produces("application/vnd.org.jfrog.artifactory.search.checksumsearchresult+json")
    public String searchByChecksum(@QueryParam("sha1") String checkSum) {
        return "{\"results\": [" + searchResultsFor(CheckSum.ofHexString(checkSum)) + "]}";
    }

    private String searchResultsFor(CheckSum checkSum) {
        if (checksumFor(FOO).equals(checkSum)) {
            return uriJar(FOO, CURRENT_FOO_VERSION);
        } else if (checksumFor(BAR).equals(checkSum)) {
            return uriJar(BAR, CURRENT_BAR_VERSION);
        } else if (FAILING_CHECKSUM.equals(checkSum)) {
            throw new RuntimeException("error in repo");
        } else if (AMBIGUOUS_CHECKSUM.equals(checkSum)) {
            return uriJar("x", null) + "," + uriJar("y", null);
        } else if (UNKNOWN_CHECKSUM.equals(checkSum)) {
            return "";
        } else {
            String name = nameFor(checkSum);
            Version version = versionFor(checkSum);
            return uriJar(name, version);
        }
    }

    public static CheckSum checksumFor(String name) {
        return checksumFor(name, versionFor(name));
    }

    public static CheckSum checksumFor(String name, Version version) {
        return CheckSum.of(("checkSum(" + name + "@" + version + ")").getBytes());
    }

    private String uriJar(String name, Version version) {
        String path = "/libs-release-local/" + name + "/" + version + "/" + name + "-" + version + ".jar";
        return "{" //
                + "\"uri\":\"" + base("api/storage" + path) + "\",\n" //
                + "\"downloadUri\" : \"" + base(path) + "\"\n" //
                + "}";
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
    @Path("/api/storage/{repository}/{path:.*}")
    @Produces("application/vnd.org.jfrog.artifactory.storage.folderinfo+json")
    public String listFiles(@PathParam("repository") String repository, @PathParam("path") String path) {
        return "{\n" //
                + "   \"path\" : \"" + path + "\",\n" //
                + "   \"createdBy\" : \"kirk\",\n" //
                + "   \"repo\" : \"" + repository + "\",\n" //
                + "   \"modifiedBy\" : \"spock\",\n" //
                + "   \"created\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                + "   \"lastUpdated\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                + "   \"children\" : [\n" //
                + versions(path) //
                + "      {\n" //
                + "         \"folder\" : false,\n" //
                + "         \"uri\" : \"/maven-metadata.xml\"\n" //
                + "      }\n" //
                + "   ],\n" //
                + "   \"lastModified\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                + "   \"uri\" : \"" + base(repository + path) + "\"\n" //
                + "}\n" //
        ;
    }

    private String versions(String path) {
        StringBuilder out = new StringBuilder();
        for (Version version : availableVersionsFor(path)) {
            out.append(version(version.getVersion()));
        }
        return out.toString();
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

    public static List<Version> availableVersionsFor(String name) {
        switch (name) {
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

    private String version(String version) {
        return "      {\n" //
                + "         \"folder\" : true,\n" //
                + "         \"uri\" : \"/" + version + "\"\n" //
                + "      },\n" //
        ;
    }

    @GET
    @Path("/{repository}/{path:.*}")
    public InputStream getFile(@SuppressWarnings("unused") @PathParam("repository") String repository,
            @PathParam("path") String pathString) {
        java.nio.file.Path path = Paths.get(pathString);
        int n = path.getNameCount();
        String name = path.getName(n - 1).toString();
        Version version = new Version(path.getName(n - 2).toString());
        return inputStreamFor(name, version);
    }

    public static InputStream inputStreamFor(String name, Version version) {
        // if ((FOO_WAR).equals(name))
        // try {
        // return Files.newInputStream(Paths.get(FOO_WAR));
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
        return new StringInputStream(name + "-content@" + version);
    }
}
