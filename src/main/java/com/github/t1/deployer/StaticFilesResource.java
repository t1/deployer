package com.github.t1.deployer;

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
 * dependency. Currently only <code>bootstrap</code> is supported, but that's trivially extended.
 * <p/>
 * This class is not suitable for heavy load. Really heavy load should be fulfilled by serving static resources from
 * e.g. Apache or even a CDN. But the performance of this class could also be improved by:
 * <ul>
 * <li>Let the clients cache, by adding etags and modified-since headers.</li>
 * <li>Cache the resources in this class.</li>
 * </ul>
 */
@Slf4j
@Path("/")
@Logged(level = TRACE)
public class StaticFilesResource {
    private static final String[] WEBJARS = { "bootstrap", "angularjs" };

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

    private final List<StaticFilesLoader> loaders = new ArrayList<>();

    public StaticFilesResource() {
        loaders.add(new WebappStaticFilesLoader());
        for (String artifact : WEBJARS) {
            addLoaderFor(artifact);
        }
    }

    private void addLoaderFor(String artifact) {
        String version = versionOf(artifact);
        if (version != null) {
            loaders.add(new WebjarFilesLoader(artifact, version));
        }
    }

    private String versionOf(String artifact) {
        URL resource = classLoader().getResource("/META-INF/maven/org.webjars/" + artifact + "/pom.properties");
        if (resource == null)
            return null;
        try (InputStream is = resource.openStream()) {
            Properties properties = new Properties();
            properties.load(is);
            assert properties.getProperty("artifactId").equals(artifact);
            return properties.getProperty("version");
        } catch (IOException e) {
            return null;
        }
    }

    public ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
            loader = getClass().getClassLoader();
        return loader;
    }

    @GET
    @Path("/{type}/{file-path:.*}")
    @SuppressWarnings("resource")
    public Response getStaticResource(@PathParam("type") String type, @PathParam("file-path") String fileName) {
        for (StaticFilesLoader loader : loaders) {
            String path = loader.prefix() + "/" + type + "/" + fileName;
            InputStream stream = classLoader().getResourceAsStream(path);
            if (stream != null) {
                log.debug("found {} {} in {}", type, fileName, loader.name());
                return Response.ok(stream).build();
            }
        }
        log.warn("not found: {}: {}", type, fileName);
        return Response //
                .status(NOT_FOUND) //
                .entity("no static " + type + " resource found: " + fileName) //
                .type(TEXT_PLAIN) //
                .build();
    }
}
