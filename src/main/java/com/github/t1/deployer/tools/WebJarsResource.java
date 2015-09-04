package com.github.t1.deployer.tools;

import static com.github.t1.log.LogLevel.*;
import static java.util.Arrays.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.log.Logged;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serve static resources from <code>src/main/resources</code> or any <a href="http://www.webjars.org">webjar</a>
 * dependency. Note that this is only necessary if you configure your JAX-RS root to be <code>/</code> which hides the
 * built-in mechanism to serve resources from <code>WEB-INF</code> and <code>META-INF/resources</code>. But it does have
 * the additional benefit that you don't have to put, e.g., <code>webjars/bootstrap/3.3.2</code> into the path.
 * <p/>
 * This class is not suitable for heavy load. Really heavy load should be fulfilled by serving static resources from
 * e.g. Apache or even a CDN. But the performance of this class could also be improved by:
 * <ul>
 * <li>Let the clients cache, by adding etags (e.g. the version number), modified-since (from the pom.properties
 * comment), and cache control headers.</li>
 * <li>Cache the resources in this class (e.g. with JCache).</li>
 * </ul>
 */
@Slf4j
@Path("/")
@Logged(level = TRACE)
public class WebJarsResource {
    private static final List<String> ALLOWED_STATIC_FOLDERS =
            asList("css", "doc", "fonts", "html", "img", "js", "scripts", "styles");

    @RequiredArgsConstructor
    private class StaticFilesLoader {
        final String name;
        final String prefix;

        public StaticFilesLoader(String name) {
            this(name, "");
        }

        @SuppressWarnings("resource")
        public Response response(String filePath) {
            String path = prefix + filePath;
            InputStream stream = classLoader().getResourceAsStream(path);
            if (stream == null)
                return notFound("resource '" + filePath + "' not found in '" + name + "'");
            log.debug("found {} in {}", filePath, name);
            return Response.ok(stream).type(type(fileSuffix(filePath))).build();
        }

        private String fileSuffix(String filePath) {
            if (filePath == null)
                return null;
            int i = filePath.lastIndexOf('.');
            if (i < 0)
                return null;
            return filePath.substring(i);
        }

        private MediaType type(String fileSuffix) {
            if (fileSuffix == null)
                return null;
            switch (fileSuffix) {
                case ".css":
                    return MediaType.valueOf("text/css");
                case ".html":
                    return TEXT_HTML_TYPE;

                case ".gif":
                    return MediaType.valueOf("image/gif");
                case ".ico":
                    return MediaType.valueOf("image/x-icon");
                case ".jpeg":
                    return MediaType.valueOf("image/jpeg");
                case ".png":
                    return MediaType.valueOf("image/png");

                case ".raml":
                    return MediaType.valueOf("application/raml+yaml");

                default:
                    return TEXT_PLAIN_TYPE;
            }
        }
    }

    private class NotFoundLoader extends StaticFilesLoader {
        public NotFoundLoader(String name) {
            super(name);
        }

        @Override
        public Response response(String filePath) {
            return notFound("no static resource found for " + name + ". "
                    + "Note that we serve only webjars and these resource folder: " + ALLOWED_STATIC_FOLDERS);
        }
    }

    private class WebjarFilesLoader extends StaticFilesLoader {
        public WebjarFilesLoader(String artifact, String version) {
            super(artifact + " webjar", "META-INF/resources/webjars/" + artifact + "/" + version + "/");
        }
    }

    private final Map<String, StaticFilesLoader> loaders = new HashMap<>();

    @GET
    @Path("favicon.ico")
    public Response getFavicon() {
        return new StaticFilesLoader("favicon").response("favicon.ico");
    }

    @GET
    @Path("/{artifact}/{filePath:.*}")
    public Response getStaticResource(@PathParam("artifact") String artifact, @PathParam("filePath") String filePath) {
        StaticFilesLoader loader = getLoaderFor(artifact);
        if (loader == null)
            return notFound("artifact not found '" + artifact + "' (for path '" + filePath + "')");
        log.debug("serve {} from {}", filePath, loader.name);
        return loader.response(filePath);
    }

    private Response notFound(String message) {
        log.warn("not found: {}", message);
        return Response.status(NOT_FOUND).entity(message + "\n").type(TEXT_PLAIN).build();
    }

    private StaticFilesLoader getLoaderFor(String artifact) {
        StaticFilesLoader loader = loaders.get(artifact);
        if (loader == null) {
            loader = createLoaderFor(artifact);
            loaders.put(artifact, loader);
        }
        return loader;
    }

    private StaticFilesLoader createLoaderFor(String artifact) {
        if (ALLOWED_STATIC_FOLDERS.contains(artifact))
            return new StaticFilesLoader(artifact, artifact + "/");
        String version = versionOf(artifact);
        if (version == null)
            return new NotFoundLoader(artifact);
        return new WebjarFilesLoader(artifact, version);
    }

    private String versionOf(String artifact) {
        String path = "/META-INF/maven/org.webjars/" + artifact + "/pom.properties";
        URL resource = classLoader().getResource(path);
        if (resource == null) {
            log.debug("no pom properties found in {}", path);
            return null;
        }
        try (InputStream is = resource.openStream()) {
            Properties properties = new Properties();
            properties.load(is);
            assert properties.getProperty("artifactId").equals(artifact);
            String version = properties.getProperty("version");
            log.debug("found version {} for {}", version, path);
            return version;
        } catch (IOException e) {
            log.debug("exception while loading {}: {}", path, e);
            return null;
        }
    }

    private ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
            loader = getClass().getClassLoader();
        return loader;
    }
}
