package com.github.t1.deployer.tools;

import static com.github.t1.log.LogLevel.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.github.t1.log.Logged;

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
    private abstract class StaticFilesLoader {
        public String prefix() {
            return "";
        }

        public abstract String name();
    }

    private class WebappStaticFilesLoader extends StaticFilesLoader {
        @Override
        public String name() {
            return "webapp";
        }
    }

    @RequiredArgsConstructor
    private class WebjarFilesLoader extends StaticFilesLoader {
        private final String artifact;
        private final String version;

        @Override
        public String prefix() {
            return "META-INF/resources/webjars/" + artifact + "/" + version;
        }

        @Override
        public String name() {
            return artifact + " webjar";
        }
    }

    private final Map<String, StaticFilesLoader> loaders = new HashMap<>();

    @GET
    @Path("/{artifact}/{file-path:.*}")
    @SuppressWarnings("resource")
    public Response getStaticResource(@PathParam("artifact") String artifact, @PathParam("file-path") String filePath) {
        StaticFilesLoader loader = getLoaderFor(artifact);
        if (loader == null)
            return notFound("artifact not found '" + artifact + "' (for path '" + filePath + "')");
        String path = loader.prefix() + "/" + filePath;
        InputStream stream = classLoader().getResourceAsStream(path);
        if (stream != null) {
            log.debug("found {} in {}", filePath, loader.name());
            return Response.ok(stream).build();
        }
        return notFound("resource '" + filePath + "' not found in '" + artifact + "'");
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
        if ("webapp".equals(artifact))
            return new WebappStaticFilesLoader();
        String version = versionOf(artifact);
        if (version == null)
            return null;
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
