package com.github.t1.deployer.repository;

import io.dropwizard.testing.junit.DropwizardClientRule;

import java.net.URI;

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
        ArtifactoryRepository repo = initRepo(dropwizard.baseUri());

        try {
            // Deployment deployment = repo.getByChecksum(CHECKSUM);
            for (VersionInfo version : repo.availableVersionsFor(CHECKSUM)) {
                System.out.println("-> " + version);
            }
        } finally {
            dropwizard.stop();
        }
    }

    private static ArtifactoryRepository initRepo(URI uri) {
        ArtifactoryRepository repo = new ArtifactoryRepository();
        // FIXME repo.baseUri = UriBuilder.fromUri(uri).path("artifactory").build();
        repo.init();
        return repo;
    }
}
