package com.github.t1.deployer.repository;

import io.dropwizard.testing.junit.DropwizardClientRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class RepositoryProducerTest {
    @Test
    public void shouldLookupMavenCentralRepository() throws Exception {
        RepositoryProducer producer = new RepositoryProducer();

        Repository repository = producer.repository();

        assertThat(repository).isInstanceOf(MavenCentralRepository.class);
    }

    @Test
    public void shouldLookupLocalArtifactoryRepository() throws Throwable {
        MyDropwizardClientRule rule = new MyDropwizardClientRule();
        rule.before();
        try {
            RepositoryProducer producer = new RepositoryProducer();
            producer.uri = rule.baseUri();

            Repository repository = producer.repository();

            assertThat(repository).isInstanceOf(ArtifactoryRepository.class);
        } finally {
            rule.after();
        }
    }

    private static class MyDropwizardClientRule extends DropwizardClientRule {
        public MyDropwizardClientRule() {super(new ArtifactoryMock());}

        @Override public void before() throws Throwable { super.before(); }

        @Override public void after() { super.after(); }
    }
}
