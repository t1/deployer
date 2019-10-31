package com.github.t1.deployer.repository;

import org.junit.jupiter.api.BeforeAll;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assumptions.assumeThat;

public class MavenCentralIT extends MavenCentralTestParent {
    private static final URI MAVEN_CENTRAL = URI.create("https://search.maven.org");

    @Override protected URI baseUri() { return MAVEN_CENTRAL; }

    @BeforeAll
    public static void setUp() {
        try {
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(MAVEN_CENTRAL.getHost());
        } catch (UnknownHostException e) {
            assumeThat(e).isNull();
        }
    }
}
