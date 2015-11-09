package com.github.t1.deployer;

import static java.util.Arrays.*;

import java.util.List;

import com.github.t1.deployer.app.Deployments;
import com.github.t1.deployer.app.html.DeployerPage;
import com.github.t1.deployer.tools.*;

public class PackageDependenciesTest extends AbstractPackageDependenciesTest {
    @Override
    public List<Class<?>> getDependencyEntryPoints() {
        return asList( //
                Deployments.class, // app
                DeployerPage.class, // app.html
                ConfigProducer.class // tools
        );
    }
}
