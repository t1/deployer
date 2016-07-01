package com.github.t1.deployer.app;

import com.github.t1.log.Logged;

import java.util.*;

import static com.github.t1.log.LogLevel.*;

public class Audits {
    private final List<Audit> audits = new ArrayList<>();

    @Logged(level = INFO)
    public void audit(Audit audit) { this.audits.add(audit); }

    public List<Audit> asList() { return audits; }
}
