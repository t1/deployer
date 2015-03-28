package com.github.t1.deployer.app;

import static javax.interceptor.Interceptor.Priority.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

import java.security.Principal;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.Deployment;

/**
 * Cross cutting concerns for updating the deployment status in a container:
 * <ul>
 * <li>Check privileges of the principal.</li>
 * <li>Write autit log.</li>
 * <li>Update the list of deployments.</li>
 * </ul>
 */
@Slf4j
@Interceptor
@ContainerDeployment
@Priority(APPLICATION + 523)
public class DeploymentUpdateInterceptor {
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
        if (!isAllowed()) {
            audit.deny(operation, deployment.getContextRoot(), deployment.getVersion());
            String message = principal.getName() + " is not allowed to perform " + operation + " on " + deployment;
            Response response = Response.status(UNAUTHORIZED).entity(message).type(TEXT_PLAIN).build();
            throw new WebApplicationException(response);
        }
        audit.allow(operation, deployment.getContextRoot(), deployment.getVersion());
        context.proceed();
        deploymentsList.writeDeploymentsList();
        return null;
    }

    private boolean isAllowed() {
        return true;
    }
}
