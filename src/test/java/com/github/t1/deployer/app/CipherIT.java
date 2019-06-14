package com.github.t1.deployer.app;

import com.github.t1.deployer.tools.CipherService;
import com.github.t1.deployer.tools.KeyStoreConfig;
import io.dropwizard.testing.junit.DropwizardClientRule;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class CipherIT {
    private static final Path KEYSTORE = Paths.get("/tmp/cipher-it.keystore");

    @ClassRule
    public static DropwizardClientRule dropwizard = new DropwizardClientRule(bindings());

    @NotNull private static Object[] bindings() {
        return new Object[]{
            CipherBoundary.class,
            CipherService.class,
            new KeyStoreConfig()
                .setPath(KEYSTORE.toString())
                .setAlias("keypair")
        };
    }

    private static String read(URI uri) {
        return ClientBuilder.newClient()
            .target(uri)
            .request()
            .post(Entity.entity("some string", TEXT_PLAIN_TYPE))
            .readEntity(String.class);
    }

    @Before public void setUp() throws Exception { Files.copy(Paths.get("src/test/resources/jks.keystore"), KEYSTORE); }

    @After public void tearDown() throws Exception { Files.deleteIfExists(KEYSTORE); }

    @Test public void shouldEncrypt() {
        String response = read(URI.create(dropwizard.baseUri() + "/ciphers/encrypt"));

        assertThat(response).matches("\\p{XDigit}{512}");
    }
}
