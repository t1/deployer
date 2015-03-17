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

    @Logged("{identity};deploy;{contextRoot};{version};{host}")
    public void deploy(ContextRoot contextRoot, Version version) {}

    @Logged("{identity};redeploy;{contextRoot};{version};{host}")
    public void redeploy(ContextRoot contextRoot, Version version) {}

    @Logged("{identity};undeploy;{contextRoot};{version};{host}")
    public void undeploy(ContextRoot contextRoot, Version version) {}
}
