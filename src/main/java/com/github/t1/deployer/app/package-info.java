@DependsUpon(packagesOf = { //
        com.github.t1.deployer.model.Deployment.class, //
        com.github.t1.deployer.container.DeploymentContainer.class, //
        com.github.t1.ramlap.tools.ProblemDetail.class, //
        com.github.t1.deployer.app.file.DeploymentListFile.class, //
})
@com.github.t1.ramlap.annotations.ApiGenerate(from = "src/main/resources/doc/deployer.raml")
package com.github.t1.deployer.app;

import com.github.t1.deployer.tools.DependsUpon;
