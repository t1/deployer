package com.github.t1.deployer.repository;

import org.junit.jupiter.api.BeforeAll;

import java.net.URI;

import static com.github.t1.deployer.testtools.NetworkTestTools.assumeKnownHost;

public class MavenCentralIT extends MavenCentralTestParent {
    private static final URI MAVEN_CENTRAL = URI.create("https://search.maven.org");

    @Override protected URI baseUri() { return MAVEN_CENTRAL; }

    @BeforeAll static void setUp() {
        assumeKnownHost(MAVEN_CENTRAL.getHost());
    }
}
