package com.github.t1.deployer.app;

import static javax.interceptor.Interceptor.Priority.*;

import java.security.Principal;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.*;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.deployer.app.file.DeploymentListFile;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.Deployment;
import com.github.t1.deployer.tools.UnauthorizedException;

/**
 * Cross cutting concerns for updating the deployment status in a container:
 * <ul>
 * <li>Check privileges of the current user.</li>
 * <li>Write autit log.</li>
 * <li>Update the list of deployments.</li>
 * </ul>
 */
@Slf4j
@Interceptor
@DeploymentOperation
@Priority(APPLICATION + 523)
public class DeploymentOperationInterceptor {
    @Inject
    Audit audit;
    @Inject
    DeploymentListFile deploymentsList;
    @Inject
    Principal principal;

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        Deployment deployment = (Deployment) context.getParameters()[0];
        String operation = context.getMethod().getName();
        log.debug("intercept {} of {} by {}", operation, deployment, principal.getName());
        if (!principal.getName().equals("jbossadmin")) {
            audit.deny(operation, deployment.getContextRoot(), deployment.getVersion());
            throw new UnauthorizedException(principal.getName(), operation, deployment);
        }
        audit.allow(operation, deployment.getContextRoot(), deployment.getVersion());
        Object result = context.proceed();
        deploymentsList.writeDeploymentsList();
        return result;
    }
}
