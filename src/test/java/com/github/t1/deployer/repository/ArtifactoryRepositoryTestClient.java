package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.rest.RestContext;
import io.dropwizard.testing.junit.DropwizardClientRule;
import lombok.SneakyThrows;

import static com.github.t1.rest.RestContext.REST;

public class ArtifactoryRepositoryTestClient {
    private static final Checksum CHECKSUM = Checksum.ofHexString("CC99BBEC4AF60E0A39AE4CA4312001B29287580C");

    private static final class Dropwizard extends DropwizardClientRule {
        private Dropwizard() {
            super(new ArtifactoryMock());
        }

        @SneakyThrows(Throwable.class)
        public Dropwizard start() {
            super.before();
            return this;
        }

        public void stop() {
            super.after();
        }
    }

    public static void main(String[] args) {
        Dropwizard dropwizard = new Dropwizard().start();
        String artifactory = dropwizard.baseUri() + "/artifactory";
        RestContext rest = REST.register("artifactory", artifactory);
        ArtifactoryRepository repo = new ArtifactoryRepository(rest, "snapshots", "releases");

        try {
            Artifact artifact = repo.searchByChecksum(CHECKSUM);
            System.out.println("-> " + artifact);
        } finally {
            dropwizard.stop();
        }
    }
}
