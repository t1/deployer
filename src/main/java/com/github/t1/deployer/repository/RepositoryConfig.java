package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Password;
import lombok.Data;
import lombok.experimental.Accessors;

import java.net.URI;

@Data
@Accessors(chain = true)
public class RepositoryConfig {
    RepositoryType type;
    URI uri;
    String username;
    Password password;
    String snapshots;
    String releases;
}
