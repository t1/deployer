package com.github.t1.deployer.container;

import static com.github.t1.log.LogLevel.*;

import java.net.*;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.deployer.model.*;
import com.github.t1.log.*;

@Slf4j
@SuppressWarnings("unused")
public class Audit {
    private static final String LOG_LINE = ";{client-ip};{operation};{contextRoot};{version};{host}";

    private static InetAddress getLocalHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.warn("can't get local host", e);
            return InetAddress.getLoopbackAddress();
        }
    }

    // @LogContext
    // User user = User.getCurrent();

    @LogContext
    String host = getLocalHost().getHostName();

    @Logged(value = "allow" + LOG_LINE, level = INFO)
    public void allow(String operation, ContextRoot contextRoot, Version version) {}

    @Logged(value = "deny" + LOG_LINE, level = WARN)
    public void deny(String operation, ContextRoot contextRoot, Version version) {}
}
