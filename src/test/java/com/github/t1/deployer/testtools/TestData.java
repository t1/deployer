package com.github.t1.deployer.testtools;

import com.github.t1.deployer.app.Audit.ArtifactAudit;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;

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

    public static ArtifactAuditBuilder artifactAuditOf(String groupId, String artifactId, String version) {
        return artifactAuditOf(new GroupId(groupId), new ArtifactId(artifactId), new Version(version));
    }

    public static ArtifactAuditBuilder artifactAuditOf(GroupId groupId, ArtifactId artifactId, Version version) {
        return ArtifactAudit.builder().groupId(groupId).artifactId(artifactId).version(version);
    }
}
