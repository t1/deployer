package com.github.t1.deployer.app;

//import com.github.t1.deployer.model.Config;
//import com.github.t1.deployer.tools.*;
//import com.github.t1.testtools.WebArchiveBuilder;
//import org.apache.http.client.fluent.Request;
//import org.jboss.shrinkwrap.api.spec.WebArchive;
//import org.junit.*;
//import org.junit.runner.RunWith;
//
//import javax.enterprise.inject.Produces;
//import java.io.IOException;
//import java.net.URI;
//import java.nio.file.*;
//
//import static com.github.t1.deployer.app.KeyStoreConfigProducer.*;
//import static org.assertj.core.api.Assertions.*;

//@Ignore
//@RunWith(Arquillian.class)
public class CipherIT {
//    private static final Path TEST_KEYSTORE = Paths.get("src/test/resources/jks.keystore");
//
//    @Deployment(testable = false)
//    public static WebArchive createDeployment() {
//        return new WebArchiveBuilder("cipher-it.war")
//                .with(CipherBoundary.class, CipherFacade.class, KeyStoreConfig.class, RestApplication.class)
//                .with(KeyStoreConfigProducer.class, Config.class)
//                .withBeansXml()
//                .print().build();
//    }
//
//    private static String read(URI uri) throws IOException {
//        return Request.Post(uri).execute().returnContent().asString();
//    }
//
//    @Before public void setUp() throws Exception { Files.copy(TEST_KEYSTORE, TMP_PATH); }
//
//    @After public void tearDown() throws Exception { Files.deleteIfExists(TMP_PATH); }
//
//    @ArquillianResource URI baseUri;
//
//    @Test
//    public void shouldEncrypt() throws Exception {
//        String response = read(baseUri.resolve("ciphers/encrypt"));
//
//        assertThat(response).matches("\\p{XDigit}{512}");
//    }
//}
//
//class KeyStoreConfigProducer {
//    static final KeyStoreConfig KEY_STORE_CONFIG = KeyStoreConfig
//            .builder()
//            .path("/tmp/cipher-it.keystore")
//            .alias("keypair")
//            .build();
//    static final Path TMP_PATH = Paths.get(KEY_STORE_CONFIG.getPath());
//
//    @Produces @Config("key-store")
//    public KeyStoreConfig produceKeyStoreConfig() {
//        return KEY_STORE_CONFIG;
//    }
}
