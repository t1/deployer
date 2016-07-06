package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;

public class ConfigurationPlanSerializationTest {
    @Test
    public void shouldLoadInSequence() throws Exception {
        String A = "  A:\n"
                + "    level: ALL\n"
                + "    file: deployer-audit.log\n"
                + "    suffix: .yyyy-MM-dd\n"
                + "    format: \"%d{yyyy-MM-dd HH:mm:ss,SSS}|%X{host}|%X{slot}|%X{version}|%X{client}|%t|%X{reference}|%c|%p|%s%e%n\"\n";
        String B = "  B:\n"
                + "    level: ALL\n"
                + "    file: deployer.log\n"
                + "    suffix: .yyyy-MM-dd\n"
                + "    format: \"%d{yyyy-MM-dd HH:mm:ss,SSS}|%X{host}|%X{slot}|%X{version}|%X{client}|%t|%X{reference}|%c|%p|%s%e%n\"\n";
        ConfigurationPlan planAB = ConfigurationPlan.load(new StringReader(""
                + "log-handlers:\n"
                + A
                + B
        ));
        ConfigurationPlan planBA = ConfigurationPlan.load(new StringReader(""
                + "log-handlers:\n"
                + B
                + A
        ));

        assertThat(planAB.loggers().collect(toList())).isEmpty();
        assertThat(planAB.deployments().collect(toList())).isEmpty();
        assertThat(handlerNames(planAB)).containsExactly("A", "B");

        assertThat(planBA.loggers().collect(toList())).isEmpty();
        assertThat(planBA.deployments().collect(toList())).isEmpty();
        assertThat(handlerNames(planBA)).containsExactly("B", "A");
    }

    public List<String> handlerNames(ConfigurationPlan planBA) {
        return planBA.logHandlers()
                     .map(LogHandlerConfig::getName)
                     .map(LogHandlerName::getValue)
                     .collect(toList());
    }

    @Test
    public void shouldSerializeEmptyPlan() throws Exception {
        ConfigurationPlan plan = ConfigurationPlan.builder().build();

        String yaml = plan.toYaml();

        assertThat(yaml).isEqualTo("{}\n");
    }

    @Test
    public void shouldSerializePlanWithOneDeployment() throws Exception {
        DeploymentConfig foo = DeploymentConfig
                .builder()
                .groupId(new GroupId("org.foo"))
                .artifactId(new ArtifactId("foo-war"))
                .version(new Version("1"))
                .type(war)
                .deploymentName(new DeploymentName("foo"))
                .state(deployed)
                .build();
        ConfigurationPlan plan = ConfigurationPlan
                .builder()
                .deployment(foo.getDeploymentName(), foo)
                .build();

        String yaml = plan.toYaml();

        assertThat(yaml).isEqualTo("deployments:\n"
                + "  foo:\n"
                + "    groupId: org.foo\n"
                + "    artifactId: foo-war\n"
                + "    state: deployed\n"
                + "    deploymentName: foo\n"
                + "    version: 1\n"
                + "    type: war\n");
    }
}
