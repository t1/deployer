@DependsUpon(packagesOf = { //
        com.github.t1.deployer.model.Deployment.class, //
        com.github.t1.deployer.container.DeploymentContainer.class, //
        com.github.t1.ramlap.tools.ProblemDetail.class, //
        com.github.t1.deployer.app.file.DeploymentListFile.class, //
        io.swagger.config.Scanner.class, //
        io.swagger.jaxrs.Reader.class, //
        io.swagger.jaxrs.config.BeanConfig.class, //
        io.swagger.core.filter.SwaggerSpecFilter.class, //
        io.swagger.models.Swagger.class, //
})
@com.github.t1.ramlap.annotations.ApiGenerate(from = "src/main/resources/doc/deployer2.raml")
package com.github.t1.deployer.app;

import com.github.t1.deployer.tools.DependsUpon;
