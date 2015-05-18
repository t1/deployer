package com.github.t1.deployer.app;

import static javax.interceptor.Interceptor.Priority.*;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.*;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.deployer.app.file.DeploymentListFile;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.Deployment;
import com.github.t1.deployer.tools.*;

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

    User user = User.getCurrent();

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        Deployment deployment = (Deployment) context.getParameters()[0];
        String operation = context.getMethod().getName();
        log.debug("intercept {} of {} by {}", operation, deployment, user.getName());
        if (!user.hasPrivilege(operation)) {
            audit.deny(operation, deployment.getContextRoot(), deployment.getVersion());
            throw new UnauthorizedException(user, operation, deployment);
        }
        audit.allow(operation, deployment.getContextRoot(), deployment.getVersion());
        Object result = context.proceed();
        deploymentsList.writeDeploymentsList();
        return result;
    }
}
