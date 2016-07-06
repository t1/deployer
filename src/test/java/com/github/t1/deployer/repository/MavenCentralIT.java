package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Checksum;
import org.junit.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.rest.RestContext.*;
import static org.assertj.core.api.Assertions.*;

@Ignore
public class MavenCentralIT {
    private final MavenCentralRepository repository = new MavenCentralRepository();

    @Before
    public void setUp() throws Exception {
        this.repository.rest = REST;
    }

    @Test
    public void shouldDeserializeSearchByChecksumResult() throws Exception {
        Checksum checksum = Checksum.fromString("f6e5786754116cc8e1e9261b2a117701747b1259");

        Artifact artifact = repository.getByChecksum(checksum);

        assertThat(artifact.getGroupId().getValue()).isEqualTo("org.jolokia");
        assertThat(artifact.getArtifactId().getValue()).isEqualTo("jolokia-war");
        assertThat(artifact.getVersion().getValue()).isEqualTo("1.3.3");
        assertThat(artifact.getType()).isEqualTo(war);
        assertThat(artifact.getChecksum()).isEqualTo(checksum);
    }
}
