package com.github.t1.deployer.testtools;

import com.github.t1.deployer.container.*;

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

    public static void assertDeployment(ContextRoot expectedContextRoot, Deployment actualDeployment) {
        assertEquals(expectedContextRoot, actualDeployment.getContextRoot());
        assertEquals(nameFor(expectedContextRoot), actualDeployment.getName());
    }
}
