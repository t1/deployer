package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.rest.RestClientMocker;
import com.github.t1.rest.RestContext;
import org.junit.Test;

import static com.github.t1.deployer.model.ArtifactType.war;
import static com.github.t1.deployer.repository.RepositoryProducer.DEFAULT_MAVEN_CENTRAL_URI;
import static com.github.t1.deployer.repository.RepositoryProducer.REST_ALIAS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

public class MavenCentralTest {
    private final RestClientMocker mock = new RestClientMocker()
            .register(REST_ALIAS, DEFAULT_MAVEN_CENTRAL_URI);
    private final RestContext rest = mock.context();
    private final MavenCentralRepository repository = new MavenCentralRepository(rest);

    @Test
    public void shouldDeserializeSearchByChecksumResult() throws Exception {
        Checksum checksum = Checksum.fromString("f6e5786754116cc8e1e9261b2a117701747b1259");
        mock.on(repository.searchUri().with("checksum", checksum))
            .GET()
            .respond("{\n"
                    + "    \"response\": {\n"
                    + "        \"docs\": [\n"
                    + "            {\n"
                    + "                \"a\": \"jolokia-war\", \n"
                    + "                \"ec\": [\n"
                    + "                    \".war\", \n"
                    + "                    \".pom\"\n"
                    + "                ], \n"
                    + "                \"g\": \"org.jolokia\", \n"
                    + "                \"id\": \"org.jolokia:jolokia-war:1.3.3\", \n"
                    + "                \"p\": \"war\", \n"
                    + "                \"tags\": [\n"
                    + "                    \"application\", \n"
                    + "                    \"agent\"\n"
                    + "                ], \n"
                    + "                \"timestamp\": 1455653012000, \n"
                    + "                \"v\": \"1.3.3\"\n"
                    + "            }\n"
                    + "        ], \n"
                    + "        \"numFound\": 1, \n"
                    + "        \"start\": 0\n"
                    + "    }, \n"
                    + "    \"responseHeader\": {\n"
                    + "        \"QTime\": 0, \n"
                    + "        \"params\": {\n"
                    + "            \"fl\": \"id,g,a,v,p,ec,timestamp,tags\", \n"
                    + "            \"indent\": \"off\", \n"
                    + "            \"q\": \"1:\\\"f6e5786754116cc8e1e9261b2a117701747b1259\\\"\", \n"
                    + "            \"rows\": \"20\", \n"
                    + "            \"sort\": \"score desc,timestamp desc,g asc,a asc,v desc\", \n"
                    + "            \"version\": \"2.2\", \n"
                    + "            \"wt\": \"json\"\n"
                    + "        }, \n"
                    + "        \"status\": 0\n"
                    + "    }\n"
                    + "}", APPLICATION_JSON_TYPE);

        Artifact artifact = repository.searchByChecksum(checksum);

        assertThat(artifact.getGroupId().getValue()).isEqualTo("org.jolokia");
        assertThat(artifact.getArtifactId().getValue()).isEqualTo("jolokia-war");
        assertThat(artifact.getVersion().getValue()).isEqualTo("1.3.3");
        assertThat(artifact.getType()).isEqualTo(war);
        assertThat(artifact.getChecksum()).isEqualTo(checksum);
    }
}
