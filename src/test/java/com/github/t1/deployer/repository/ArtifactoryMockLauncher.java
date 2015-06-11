package com.github.t1.deployer.repository;

import static ch.qos.logback.classic.Level.*;
import io.dropwizard.*;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Environment;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.*;

import com.codahale.metrics.health.HealthCheck;

/**
 * If you don't have a real Artifactory Pro available, launch this, which will start a more or less working mock of
 * artifactory based on the local Maven repository in <code>~/.m2</code>. The checksums are read from an index file that
 * can be created with the {@link ArtifactoryMockIndexBuilder}.
 */
public class ArtifactoryMockLauncher extends Application<Configuration> {
    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            args = new String[] { "server" };
        new ArtifactoryMockLauncher().run(args);
    }

    private static class DummyHealthCheck extends HealthCheck {
        @Override
        protected Result check() {
            return Result.healthy();
        }
    }

    @Override
    public void run(Configuration configuration, Environment environment) {
        SimpleServerFactory serverConfig = new SimpleServerFactory();
        serverConfig.setApplicationContextPath("");
        configuration.setServerFactory(serverConfig);

        final HttpConnectorFactory connectorConfig = (HttpConnectorFactory) serverConfig.getConnector();
        connectorConfig.setPort(8081);

        environment.healthChecks().register("dummy", new DummyHealthCheck());

        environment.jersey().register(new ArtifactoryMock());
    }

    public ArtifactoryMockLauncher() {
        // ArtifactoryMock.FAKES = true;

        setLogLevel("org.apache.http.wire", DEBUG);
        setLogLevel("com.github.t1.rest", DEBUG);
        setLogLevel("com.github.t1.deployer", DEBUG);
    }

    private void setLogLevel(String loggerName, Level level) {
        ((Logger) LoggerFactory.getLogger(loggerName)).setLevel(level);
    }
}
