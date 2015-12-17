package com.github.t1.deployer;

import static java.util.Arrays.*;

import java.util.List;

import com.github.t1.testtools.AbstractPackageDependenciesTest;

public class PackageDependenciesTest extends AbstractPackageDependenciesTest {
    @Override
    public List<Class<?>> getDependencyEntryPoints() {
        return asList( //
                com.github.t1.deployer.app.Deployments.class, //
                com.github.t1.deployer.app.html.DeployerPage.class, //
                com.github.t1.deployer.tools.HttpFilter.class //
        );
    }
}
