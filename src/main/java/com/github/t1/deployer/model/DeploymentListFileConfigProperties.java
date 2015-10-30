package com.github.t1.deployer.model;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.t1.deployer.model.Config.DeploymentListFileConfig;

import lombok.Value;

@Value
public class DeploymentListFileConfigProperties {
    public static DeploymentListFileConfigProperties of(DeploymentListFileConfig config) {
        return new DeploymentListFileConfigProperties(() -> Optional.ofNullable(config));
    }

    private Supplier<Optional<DeploymentListFileConfig>> supplier;

    public Property<Boolean> autoUndeploy() {
        return new Property<>(() -> supplier.get().map(c -> c.autoUndeploy()));
    }
}
