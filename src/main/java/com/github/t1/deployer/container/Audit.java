package com.github.t1.deployer.container;

import static com.github.t1.log.LogLevel.*;

import java.net.*;

import lombok.SneakyThrows;

import com.github.t1.deployer.model.*;
import com.github.t1.deployer.tools.User;
import com.github.t1.log.*;

@SuppressWarnings("unused")
public class Audit {
    private static final String LOG_LINE = ";{user};{client-ip};{operation};{contextRoot};{version};{host}";

    @SneakyThrows(UnknownHostException.class)
    private static InetAddress getLocalHost() {
        return InetAddress.getLocalHost();
    }

    @LogContext
    User user = User.getCurrent();

    @LogContext
    String host = getLocalHost().getHostName();

    @Logged(value = "allow" + LOG_LINE, level = INFO)
    public void allow(String operation, ContextRoot contextRoot, Version version) {}

    @Logged(value = "deny" + LOG_LINE, level = WARN)
    public void deny(String operation, ContextRoot contextRoot, Version version) {}
}
