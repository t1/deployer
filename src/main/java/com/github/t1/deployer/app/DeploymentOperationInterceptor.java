package com.github.t1.deployer.app;

import static javax.interceptor.Interceptor.Priority.*;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.*;

import com.github.t1.deployer.app.file.DeploymentListFile;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.DeploymentName;

/**
 * Cross cutting concerns for updating the deployment status in a container:
 * <ul>
 * <li>Check privileges of the current user.</li>
 * <li>Write autit log.</li>
 * <li>Update the list of deployments.</li>
 * </ul>
 */
@Interceptor
@DeploymentOperation
@Priority(APPLICATION + 523)
public class DeploymentOperationInterceptor {
    @Inject
    Audit audit;
    @Inject
    DeploymentListFile deploymentsList;

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        DeploymentName deploymentName = (DeploymentName) context.getParameters()[0];
        String operation = context.getMethod().getName();
        audit.allow(operation, deploymentName);
        Object result = context.proceed();
        deploymentsList.writeDeploymentsList();
        return result;
    }
}
