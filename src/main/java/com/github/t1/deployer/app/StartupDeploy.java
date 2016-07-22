package com.github.t1.deployer.app;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.inject.Inject;

@Startup
@Singleton
@Slf4j
public class StartupDeploy {
    private static final String BOOT_IN_PROCESS
            = "WFLYCTL0379: System boot is in process; execution of remote management operations is not currently available";
    @Inject DeployerBoundary deployer;

    @PostConstruct public void startup() {
        log.info("--------------------------------------- startup deployer");
        while (true) {
            try {
                deployer.post();
                break;
            } catch (RuntimeException e) {
                String messageStack = messageStack(e);
                if (messageStack.contains(BOOT_IN_PROCESS)) {
                    log.info("--------------------------------------- retry startup\n{}", messageStack);
                } else {
                    log.error("--------------------------------------- startup FAILED\n{}", messageStack);
                    return;
                }
            }
        }
        log.info("--------------------------------------- startup done");
    }

    private String messageStack(RuntimeException e) {
        String messages = "";
        for (Throwable t = e; t != null; t = t.getCause())
            messages += "- " + t.getMessage() + "\n";
        return messages;
    }
}
