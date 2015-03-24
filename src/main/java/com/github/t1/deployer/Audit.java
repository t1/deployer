package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;

import java.net.*;
import java.security.Principal;

import javax.inject.Inject;

import lombok.SneakyThrows;

import com.github.t1.log.*;

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

    @Logged(value = "{identity};allow;{operation};{contextRoot};{version};{host}", level = INFO)
    public void allow(String operation, ContextRoot contextRoot, Version version) {}

    @Logged(value = "{identity};deny;{operation};{contextRoot};{version};{host}", level = WARN)
    public void deny(String operation, ContextRoot contextRoot, Version version) {}
}
