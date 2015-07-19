package com.github.t1.deployer.container;

import static com.github.t1.log.LogLevel.*;

import java.net.*;
import java.security.Principal;

import javax.inject.Inject;

import com.github.t1.deployer.model.DeploymentName;
import com.github.t1.log.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unused")
public class Audit {
    private static InetAddress getLocalHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.warn("use loopback address, as getting local host failed: {}", e.getMessage());
            return InetAddress.getLoopbackAddress();
        }
    }

    @Inject
    @LogContext
    Principal principal;

    @LogContext
    String host = getLocalHost().getHostName();

    @Logged(value = "{principal};{client-ip};{operation};{name};{host}", level = INFO)
    public void allow(String operation, DeploymentName name) {}
}
