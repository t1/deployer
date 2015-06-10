package com.github.t1.deployer.container;

import static com.github.t1.deployer.tools.Config.*;
import static com.github.t1.log.LogLevel.*;

import java.security.Principal;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.deployer.model.*;
import com.github.t1.log.*;

@Slf4j
@SuppressWarnings("unused")
public class Audit {
    private static final String LOG_LINE = ";{principal};{client-ip};{operation};{contextRoot};{version};{host}";

    @Inject
    @LogContext
    Principal principal;

    @LogContext
    String host = getLocalHost().getHostName();

    @Logged(value = "allow" + LOG_LINE, level = INFO)
    public void allow(String operation, ContextRoot contextRoot, Version version) {}

    @Logged(value = "deny" + LOG_LINE, level = WARN)
    public void deny(String operation, ContextRoot contextRoot, Version version) {}
}
