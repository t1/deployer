@DependsUpon(packagesOf = {
        com.github.t1.deployer.container.DeploymentContainer.class,
        com.github.t1.deployer.repository.Repository.class,
        com.github.t1.deployer.model.Checksum.class,

        com.github.t1.problem.ProblemDetail.class,
        com.fasterxml.jackson.core.JsonGenerator.class,
        com.fasterxml.jackson.core.type.TypeReference.class,
        com.fasterxml.jackson.annotation.JacksonAnnotation.class,
        com.fasterxml.jackson.databind.ObjectMapper.class,
        com.fasterxml.jackson.dataformat.yaml.YAMLFactory.class,
})
package com.github.t1.deployer.app;

import com.github.t1.testtools.DependsUpon;
