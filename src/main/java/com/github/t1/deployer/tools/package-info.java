@DependsUpon(packagesOf = { //
        com.fasterxml.jackson.databind.ObjectMapper.class, //
        com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml.class, // YamlMessageBodyWriter
        com.github.t1.deployer.model.Deployment.class, //
        com.github.t1.ramlap.tools.ProblemDetail.class, //
        com.github.t1.rest.fallback.ConverterTools.class, //
        com.github.t1.rest.RestResource.class, //
        org.jboss.as.controller.client.ModelControllerClient.class, // config
})
package com.github.t1.deployer.tools;
