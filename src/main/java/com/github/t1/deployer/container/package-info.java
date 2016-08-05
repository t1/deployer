@DependsUpon(packagesOf = {
        com.github.t1.deployer.model.Checksum.class,

        org.jboss.as.controller.client.helpers.standalone.DeploymentPlan.class,
        org.jboss.as.controller.client.ModelControllerClient.class,
        org.jboss.dmr.ModelNode.class,
        com.google.common.collect.ImmutableMap.class,
})
package com.github.t1.deployer.container;

import com.github.t1.testtools.DependsUpon;
