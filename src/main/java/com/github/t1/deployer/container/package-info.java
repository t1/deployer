@DependsUpon(packagesOf = {
        Checksum.class,

        org.jboss.as.controller.client.helpers.standalone.DeploymentPlan.class,
        org.jboss.as.controller.client.ModelControllerClient.class,
        org.jboss.dmr.ModelNode.class,
})
package com.github.t1.deployer.container;

import com.github.t1.deployer.model.Checksum;
import com.github.t1.testtools.DependsUpon;
