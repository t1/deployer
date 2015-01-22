package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;

import java.net.*;
import java.security.Principal;

import javax.inject.Inject;

import lombok.SneakyThrows;

import com.github.t1.log.*;

@Logged(level = INFO)
@SuppressWarnings("unused")
public class Audit {
    @SneakyThrows(UnknownHostException.class)
    private static InetAddress getLocalHost() {
        return InetAddress.getLocalHost();
    }

    @Inject
    @LogContext
    Principal identity;

    @LogContext
    String host = getLocalHost().getHostName();

    @Logged("{identity};deploy;{name};{version};{host}")
    public void deploy(String name, Version version) {}

    @Logged("{identity};undeploy;{name};{version};{host}")
    public void undeploy(String name, Version version) {}
}
