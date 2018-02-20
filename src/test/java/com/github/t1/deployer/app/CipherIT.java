package com.github.t1.deployer.app;

import com.github.t1.deployer.tools.CipherFacade;
import com.github.t1.deployer.tools.KeyStoreConfig;
import io.dropwizard.testing.junit.DropwizardClientRule;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class CipherIT {
    private static final Path KEYSTORE = Paths.get("/tmp/cipher-it.keystore");

    @ClassRule
    public static DropwizardClientRule dropwizard = new DropwizardClientRule(bindings());

    @NotNull private static Object[] bindings() {
        return new Object[]{
                CipherBoundary.class,
                CipherFacade.class,
                KeyStoreConfig.builder()
                        .path(KEYSTORE.toString())
                        .alias("keypair")
                        .build()
        };
    }

    private static String read(URI uri) throws IOException {
        return Request.Post(uri).execute().returnContent().asString();
    }

    @Before public void setUp() throws Exception { Files.copy(Paths.get("src/test/resources/jks.keystore"), KEYSTORE); }

    @After public void tearDown() throws Exception { Files.deleteIfExists(KEYSTORE); }

    @Test
    public void shouldEncrypt() throws Exception {
        String response = read(dropwizard.baseUri().resolve("ciphers/encrypt"));

        assertThat(response).matches("\\p{XDigit}{512}");
    }
}
