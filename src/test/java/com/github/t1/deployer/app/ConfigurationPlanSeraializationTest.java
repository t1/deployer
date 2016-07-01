package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.LogHandlerConfig;
import org.junit.Test;

import java.io.StringReader;

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
        assertThat(planAB.logHandlers().map(LogHandlerConfig::getName).collect(toList()))
                .containsExactly("A", "B");

        assertThat(planBA.loggers().collect(toList())).isEmpty();
        assertThat(planBA.deployments().collect(toList())).isEmpty();
        assertThat(planBA.logHandlers().map(LogHandlerConfig::getName).collect(toList()))
                .containsExactly("B", "A");
    }
}
