package com.github.t1.deployer.repository;

import io.dropwizard.testing.junit.DropwizardClientRule;

import javax.ws.rs.core.UriBuilder;

import com.github.t1.deployer.model.*;

public class ArtifactoryRepositoryTestClient {
    private static final CheckSum CHECKSUM = CheckSum.ofHexString("5064E70F510D3E01A939A344B4712C0594081BBB");

    private static final class Dropwizard extends DropwizardClientRule {
        private Dropwizard() {
            super(new ArtifactoryMock());
        }

        public Dropwizard start() throws Throwable {
            super.before();
            return this;
        }

        public void stop() {
            super.after();
        }
    }

    public static void main(String[] args) throws Throwable {
        Dropwizard dropwizard = new Dropwizard().start();
        ArtifactoryRepository repo = initRepo(dropwizard);

        try {
            // Deployment deployment = repo.getByChecksum(CHECKSUM);
            for (VersionInfo version : repo.availableVersionsFor(CHECKSUM)) {
                System.out.println("-> " + version);
            }
        } finally {
            dropwizard.stop();
        }
    }

    private static ArtifactoryRepository initRepo(Dropwizard dropwizard) {
        ArtifactoryRepository repo = new ArtifactoryRepository();
        repo.baseUri = UriBuilder.fromUri(dropwizard.baseUri()).path("artifactory").build();
        repo.init();
        return repo;
    }
}
