package com.github.t1.deployer.testtools;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.Version;

import static org.junit.Assert.*;

public class TestData {
    public static DeploymentName nameFor(ContextRoot contextRoot) {
        return new DeploymentName(contextRoot + ".war");
    }

    public static String successCli(String result) {
        return "{\n"
                + "\"outcome\" => \"success\",\n"
                + "\"result\" => " + result + "\n"
                + "}\n";
    }

    public static void assertDeployment(ContextRoot contextRoot, Version expectedVersion, Deployment deployment) {
        assertEquals(contextRoot, deployment.getContextRoot());
        // assertEquals(nameFor(contextRoot), deployment.getName());
        // assertEquals(expectedVersion, deployment.getVersion());
    }
}
