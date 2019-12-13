@DependsUpon(packagesOf = {
    com.github.t1.deployer.model.Checksum.class,

    com.google.common.collect.ImmutableMap.class,
    org.jboss.as.controller.client.ModelControllerClient.class,
    org.jboss.as.controller.client.helpers.Operations.class,
    org.jboss.dmr.ModelNode.class,
    org.wildfly.plugin.core.ServerHelper.class,
})
package com.github.t1.deployer.container;

import com.github.t1.testtools.DependsUpon;
