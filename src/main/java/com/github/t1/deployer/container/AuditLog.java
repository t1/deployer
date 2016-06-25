package com.github.t1.deployer.container;

import com.github.t1.deployer.container.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.model.Checksum;
import lombok.*;

import java.util.*;
import java.util.function.Function;

@RequiredArgsConstructor
public class AuditLog {
    public static class Watcher implements AutoCloseable {
        @Getter private List<Audit> audits = new ArrayList<>();

        @Override public void close() { audits = null; }
    }

    private final List<Watcher> activeWatchers = new ArrayList<>();

    public Watcher watching() {
        Watcher watcher = new Watcher();
        activeWatchers.add(watcher);
        return watcher;
    }

    private final Function<Checksum, ArtifactAuditBuilder> artifactLookup;

    public void deployed(Checksum checksum) { audit(artifactLookup.apply(checksum).deployed()); }

    public void undeployed(Checksum checksum) { audit(artifactLookup.apply(checksum).undeployed()); }

    private void audit(Audit audit) {
        for (Watcher watcher : activeWatchers)
            watcher.audits.add(audit);
    }
}
