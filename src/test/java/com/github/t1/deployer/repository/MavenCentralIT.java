package com.github.t1.deployer.repository;

import org.junit.BeforeClass;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assume.assumeNoException;

public class MavenCentralIT extends MavenCentralTestParent {
    private static final URI MAVEN_CENTRAL = URI.create("https://search.maven.org");

    @Override protected URI baseUri() { return MAVEN_CENTRAL; }

    @BeforeClass
    public static void setUp() {
        @SuppressWarnings("ResultOfMethodCallIgnored")
        Throwable e = catchThrowable(() -> InetAddress.getByName(MAVEN_CENTRAL.getHost()));
        if (e instanceof UnknownHostException)
            assumeNoException(e);
    }
}
