package com.github.t1.deployer.app;

import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.Version;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.github.t1.deployer.repository.RepositoryProducerTest.createMavenCentralRepository;
import static org.assertj.core.api.Assertions.assertThat;

public class VersionsListIT {
    private static final GroupId GROUP_ID = GroupId.of("org.jolokia");
    private static final ArtifactId ARTIFACT_ID = new ArtifactId("jolokia-war");

    DeployerBoundary boundary = new DeployerBoundary();

    @Before
    public void setUp() {
        boundary.repository = createMavenCentralRepository();
    }

    @Test
    public void shouldFetchJolokia() {
        List<Version> versions = boundary.getVersions(GROUP_ID, ARTIFACT_ID);

        assertThat(versions).contains(new Version("1.3.6"), new Version("1.2.0"), new Version("1.0.0"));
    }

    @Test
    public void shouldFetchUnknown() {
        List<Version> versions = boundary.getVersions(GROUP_ID, new ArtifactId("foo"));

        assertThat(versions).isEmpty();
    }
}
