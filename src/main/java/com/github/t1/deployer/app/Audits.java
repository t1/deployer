package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Variables.VariableName;
import com.github.t1.log.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

import static com.github.t1.deployer.app.ConfigurationPlan.*;
import static com.github.t1.log.LogLevel.*;
import static java.util.stream.Collectors.*;

/**
 * Collector of {@link Audit}s per request. Can't be returned directly from JAX-RS, as the serialisation by Jackson
 * fails on the helper fields in the CDI proxy object.
 */
@Slf4j
@Data
@RequestScoped
public class Audits {
    private final List<Audit> audits = new ArrayList<>();

    @Logged(level = DEBUG, returnFormat = "")
    public Audits add(Audit audit) {
        this.audits.add(audit);
        return this;
    }

    @Override public String toString() { return audits.stream().map(Audit::toString).collect(joining("\n", "", "\n")); }

    /** @param audits ugly: repeats `this`, but we want it `@Logged`! */
    @SuppressWarnings("unused")
    @Logged(level = INFO, json = JsonLogDetail.ALL)
    public void applied(Trigger trigger, Principal principal, Map<VariableName, String> variables, Audits audits) {}

    @SneakyThrows(IOException.class)
    public String toYaml() { return YAML.writeValueAsString(this); }
}
