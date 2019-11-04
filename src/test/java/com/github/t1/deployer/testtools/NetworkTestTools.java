package com.github.t1.deployer.testtools;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assumptions.assumeThat;

public class NetworkTestTools {
    public static void assumeKnownHost(String host) {
        try {
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            assumeThat(e).isNull();
        }
    }
}
