package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Password;
import lombok.*;

import java.net.URI;

import static lombok.AccessLevel.*;

@Value
@Builder
@NoArgsConstructor(access = PRIVATE, force = true)
@AllArgsConstructor(access = PRIVATE)
public class RepositoryConfig {
    RepositoryType type;
    URI uri;
    String username;
    Password password;
    String repositorySnapshots;
    String repositoryReleases;
}
