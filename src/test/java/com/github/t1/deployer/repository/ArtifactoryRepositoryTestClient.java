package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.RestContext;

import io.dropwizard.testing.junit.DropwizardClientRule;
import lombok.SneakyThrows;

public class ArtifactoryRepositoryTestClient {
    private static final CheckSum CHECKSUM = CheckSum.ofHexString("CC99BBEC4AF60E0A39AE4CA4312001B29287580C");

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
        RestContext config = new RestContext().register("artifactory", artifactory);
        ArtifactoryRepository repo = new ArtifactoryRepository(config);

        try {
            // Deployment deployment = repo.getByChecksum(CHECKSUM);
            for (VersionInfo version : repo.availableVersionsFor(CHECKSUM)) {
                System.out.println("-> " + version);
            }
        } finally {
            dropwizard.stop();
        }
    }
}
