package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.LogHandlerConfig;
import com.github.t1.deployer.container.LogHandlerName;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;

public class ConfigurationPlanSeraializationTest {
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
}
