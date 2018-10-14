package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.deployer.model.ProcessState;
import com.github.t1.log.JsonLogDetail;
import com.github.t1.log.Logged;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.t1.deployer.model.Plan.YAML;
import static com.github.t1.log.LogLevel.DEBUG;
import static com.github.t1.log.LogLevel.INFO;
import static com.github.t1.log.LogLevel.WARN;
import static java.util.stream.Collectors.joining;

/**
 * Collector of {@link Audit}s per request. Can't be returned directly from JAX-RS, as the serialisation by Jackson
 * fails on the helper fields in the CDI proxy object.
 */
@Slf4j
@Data
@Accessors(chain = true)
@RequestScoped
public class Audits {
    private final List<Audit> audits = new ArrayList<>();
    private ProcessState processState;
    private List<Warning> warnings;

    @Logged(level = DEBUG, returnFormat = "")
    public Audits add(Audit audit) {
        this.audits.add(audit);
        return this;
    }

    @Logged(level = WARN, returnFormat = "")
    public Audits add(Warning warning) {
        if (this.warnings == null)
            this.warnings = new ArrayList<>();
        this.warnings.add(warning);
        return this;
    }

    @Override public String toString() { return audits.stream().map(Audit::toString).collect(joining("\n", "", "\n")); }

    /** @param audits ugly: repeats `this`, but we want it `@Logged`! */
    @Logged(level = INFO, json = JsonLogDetail.ALL)
    public void applied(Trigger trigger, Principal principal, Map<VariableName, String> variables, Audits audits) {}

    @SneakyThrows(IOException.class)
    public String toYaml() { return YAML.writeValueAsString(this); }

    @Value
    public static class Warning {
        String text;
    }
}
