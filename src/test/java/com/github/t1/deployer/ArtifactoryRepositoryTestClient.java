package com.github.t1.deployer;

import io.dropwizard.testing.junit.DropwizardClientRule;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

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
        URI uri = UriBuilder.fromUri(dropwizard.baseUri()).path("artifactory").build();

        ArtifactoryRepository repo = new ArtifactoryRepository();
        repo.rest = new RestClient(uri);

        try {
            // Deployment deployment = repo.getByChecksum(CHECKSUM);
            List<Deployment> list = repo.availableVersionsFor(CHECKSUM);
            for (Deployment deployment : list) {
                System.out.println("-> " + deployment);
            }
        } finally {
            dropwizard.stop();
        }
    }
}
