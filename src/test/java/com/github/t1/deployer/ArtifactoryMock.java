package com.github.t1.deployer;

import static java.util.Arrays.*;

import java.io.*;
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

    private UriBuilder base(String path) {
        return uriInfo.getBaseUriBuilder().path(ArtifactoryMock.class).path(path);
    }

    @GET
    @Path("/api/search/checksum")
    @Produces("application/vnd.org.jfrog.artifactory.search.checksumsearchresult+json")
    public String searchChecksum(@QueryParam("sha1") String checkSum) {
        return "{\"results\": [" + resultsFor(CheckSum.ofHexString(checkSum)) + "]}";
    }

    private String resultsFor(CheckSum checkSum) {
        if (checksumFor(FOO).equals(checkSum)) {
            return uriJar(FOO, CURRENT_FOO_VERSION);
        } else if (checksumFor(BAR).equals(checkSum)) {
            return uriJar(BAR, CURRENT_BAR_VERSION);
        } else if (FAILING_CHECKSUM.equals(checkSum)) {
            throw new RuntimeException("error in repo");
        } else if (AMBIGUOUS_CHECKSUM.equals(checkSum)) {
            return uriJar("x", null) + "," + uriJar("y", null);
        } else { // UNKNOWN_CHECKSUM or anything else
            return "";
        }
    }

    private String uriJar(String name, Version version) {
        String path = "/libs-release-local/" + name + "/" + version + "/" + name + "-" + version + ".jar";
        return "{" //
                + "\"uri\":\"" + base("api/storage" + path) + "\",\n" //
                + "\"downloadUri\" : \"" + base(path) + "\"\n" //
                + "}";
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
                throw new IllegalArgumentException("no test data 'version' defined for " + name);
        }
    }

    public static List<Version> availableVersionsFor(String name) {
        switch (name) {
            case FOO:
                return FOO_VERSIONS;
            case BAR:
                return BAR_VERSIONS;
            default:
                throw new IllegalArgumentException("no test data 'available versions' defined for " + name);
        }
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
    public InputStream getFiles(@SuppressWarnings("unused") @PathParam("repository") String repository,
            @PathParam("path") String pathString) {
        java.nio.file.Path path = Paths.get(pathString);
        int n = path.getNameCount();
        String name = path.getName(n - 1).toString();
        Version version = new Version(path.getName(n - 2).toString());
        return inputStreamFor(name, version);
    }

    public static InputStream inputStreamFor(String name, Version version) {
        return new StringInputStream(name + "-content@" + version);
    }

    public static CheckSum checksumFor(String name) {
        return checksumFor(name, versionFor(name));
    }

    public static CheckSum checksumFor(String name, Version version) {
        return CheckSum.of(("checkSum(" + name + "@" + version + ")").getBytes());
    }
}
