package com.github.t1.deployer;

import static com.github.t1.log.LogLevel.*;

import com.github.t1.log.Logged;

@Logged(level = INFO)
@SuppressWarnings("unused")
public class Audit {
    // TODO @Inject @LogContext Identity identity;

    public void deploy(String contextRoot) {}

    public void undeploy(String deploymentName) {}
}
