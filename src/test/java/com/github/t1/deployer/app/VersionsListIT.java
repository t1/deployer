package com.github.t1.deployer.app;

import com.github.t1.deployer.model.*;
import org.junit.*;

import java.util.List;

import static com.github.t1.deployer.repository.RepositoryProducerTest.*;
import static org.assertj.core.api.Assertions.*;

public class VersionsListIT {
    private static final GroupId GROUP_ID = GroupId.of("org.jolokia");
    private static final ArtifactId ARTIFACT_ID = new ArtifactId("jolokia-war");

    DeployerBoundary boundary = new DeployerBoundary();

    @Before
    public void setUp() throws Exception {
        boundary.repository = createMavenCentralRepository();
    }

    @Test
    public void shouldFetchJolokia() throws Exception {
        List<Version> versions = boundary.getVersions(GROUP_ID, ARTIFACT_ID, false);

        assertThat(versions).contains(new Version("1.3.6"), new Version("1.2.0"), new Version("1.0.0"));
    }

    @Test
    public void shouldFetchUnknown() throws Exception {
        List<Version> versions = boundary.getVersions(GROUP_ID, new ArtifactId("foo"), false);

        assertThat(versions).isEmpty();
    }
}
