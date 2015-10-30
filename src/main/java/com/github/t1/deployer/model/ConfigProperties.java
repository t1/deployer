package com.github.t1.deployer.model;

import java.util.Optional;
import java.util.function.Supplier;

import lombok.Value;

@Value
public class ConfigProperties {
    public static ConfigProperties of(Config config) {
        return new ConfigProperties(() -> Optional.ofNullable(config));
    }

    Supplier<Optional<Config>> supplier;

    public DeploymentListFileConfigProperties deploymentListFileConfig() {
        return new DeploymentListFileConfigProperties(() -> supplier.get().map(c -> c.deploymentListFileConfig()));
    }
}
