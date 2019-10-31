package com.github.t1.deployer.app;

import com.github.t1.deployer.tools.CipherService;
import com.github.t1.deployer.tools.KeyStoreConfig;
import com.github.t1.jaxrsclienttest.JaxRsTestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

public class CipherIT {
    private static final Path KEYSTORE = Paths.get("/tmp/cipher-it.keystore");

    @RegisterExtension
    public static JaxRsTestExtension dropwizard = new JaxRsTestExtension(
        new CipherBoundary(
            new CipherService(),
            new KeyStoreConfig()
                .setPath(KEYSTORE.toString())
                .setAlias("keypair")));

    private static String read(URI uri) {
        return ClientBuilder.newClient()
            .target(uri)
            .request()
            .post(Entity.entity("some string", TEXT_PLAIN_TYPE))
            .readEntity(String.class);
    }

    @BeforeEach public void setUp() throws Exception { Files.copy(Paths.get("src/test/resources/jks.keystore"), KEYSTORE); }

    @AfterEach public void tearDown() throws Exception { Files.deleteIfExists(KEYSTORE); }

    @Test public void shouldEncrypt() {
        String response = read(URI.create(dropwizard.baseUri() + "/ciphers/encrypt"));

        assertThat(response).matches("\\p{XDigit}{512}");
    }
}
