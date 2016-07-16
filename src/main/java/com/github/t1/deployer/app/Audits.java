package com.github.t1.deployer.app;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

import static com.github.t1.deployer.app.ConfigurationPlan.*;

@Slf4j
@Data
public class Audits {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Audit> audits = new ArrayList<>();

    public Audits audit(Audit audit) {
        log.info("{}", audit);
        this.audits.add(audit);
        return this;
    }

    @SneakyThrows(IOException.class)
    public String toYaml() { return YAML.writeValueAsString(this); }
}
