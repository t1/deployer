package com.github.t1.deployer.testtools;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.github.t1.deployer.tools.CipherFacade;
import com.github.t1.deployer.tools.KeyStoreConfig;
import lombok.SneakyThrows;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.List;

import static com.github.t1.deployer.tools.CipherFacade.DEFAULT_PASS;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CipherFacadeMain {
    public static void main(String... args) { System.out.println(new CipherFacadeMain(args).run()); }

    private CipherFacadeMain(String[] args) {
        JCommander cli = new JCommander(this);
        cli.parse(args);
        if (help) {
            cli.usage();
            System.exit(1);
        }
    }

    @Parameter(names = "--help", help = true, description = "Show this help") private boolean help;

    @Parameter
    private List<String> bodies;
    @Parameter(names = "--keystore",
        description = "Path to the keystore file to use. Either this or `--uri` is mandatory.")
    private String keystore;
    @Parameter(names = "--storetype", description = "The file format of the keystore")
    private String storetype = KeyStore.getDefaultType();
    @Parameter(names = "--storepass", description = "The password required to open the keystore")
    private String storepass = DEFAULT_PASS;
    @Parameter(names = "--alias", description = "The 'name' of the key in the keystore")
    private String alias = "secret" + "key";
    @Parameter(names = "--decrypt", description = "Decrypt instead of encrypt")
    private boolean decrypt = false;
    @Parameter(names = "--uri",
        description = "Use the certificate of a 'https' server to encrypt. Either this or `--keystore` is mandatory.")
    private URI uri;

    private final CipherFacade cipher = new CipherFacade();

    private String run() {
        if (uri == null) {
            if (keystore == null)
                throw new IllegalArgumentException("require `--keystore` option (or `--uri`)");
            System.err.println((decrypt ? "decrypt" : "encrypt")
                + " with " + alias + " from " + storetype + " keystore " + keystore);
            return decrypt ? cipher.decrypt(body(), config()) : cipher.encrypt(body(), config());
        } else {
            if (decrypt)
                throw new IllegalArgumentException("can only encrypt when using --uri");
            if (!"https".equals(uri.getScheme()))
                throw new IllegalArgumentException("require 'https' scheme to get certificate from");
            System.err.println("encrypt for " + uri);
            Key key = loadCertificate(uri).getPublicKey();
            return cipher.encrypt(body(), key);
        }
    }

    @SneakyThrows(IOException.class)
    private static Certificate loadCertificate(URI uri) {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(uri.getHost(), port(uri))) {
            SSLSession session = socket.getSession();
            return session.getPeerCertificates()[0];
        }
    }

    @SuppressWarnings("MagicNumber")
    private static int port(URI uri) {
        int port = uri.getPort();
        if (port < 0)
            port = "https".equals(uri.getScheme()) ? 443 : 80;
        return port;
    }

    private String body() { return String.join(" ", bodies); }

    private KeyStoreConfig config() {
        return new KeyStoreConfig()
            .setPath(keystore)
            .setType(storetype)
            .setPass(storepass)
            .setAlias(alias);
    }
}
