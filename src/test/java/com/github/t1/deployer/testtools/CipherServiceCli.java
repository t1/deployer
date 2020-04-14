package com.github.t1.deployer.testtools;

import com.github.t1.deployer.tools.CipherService;
import com.github.t1.deployer.tools.KeyStoreConfig;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.List;

import static com.github.t1.deployer.tools.CipherService.DEFAULT_PASS;

@SuppressWarnings({"UseOfSystemOutOrSystemErr", "FieldMayBeFinal"})
@Command(description = "Calls The Deployer on a running instance to encrypt a secret with it's private key, so you can store it safely")
public class CipherServiceCli implements Runnable {
    public static void main(String... args) {
        int exitCode = new CommandLine(new CipherServiceCli()).execute(args);
        if (exitCode != 0)
            System.exit(exitCode);
    }

    @SuppressWarnings("unused")
    @Option(names = "--help", usageHelp = true, description = "Show this help message and exit") private boolean help;

    @Parameters
    private List<String> bodies;
    @Option(names = "--keystore",
        description = "Path to the keystore file to use. Either this or `--uri` is mandatory.")
    private String keystore;
    @Option(names = "--storetype", description = "The file format of the keystore")
    private String storetype = KeyStore.getDefaultType();
    @Option(names = "--storepass", description = "The password required to open the keystore")
    private String storepass = DEFAULT_PASS;
    @Option(names = "--alias", description = "The 'name' of the key in the keystore")
    private String alias = "secret" + "key";
    @Option(names = "--decrypt", description = "Decrypt instead of encrypt")
    private boolean decrypt = false;
    @Option(names = "--uri",
        description = "Use the certificate of a 'https' server to encrypt. Either this or `--keystore` is mandatory.")
    private URI uri;

    private final CipherService cipher = new CipherService();

    @Override public void run() {
        if (uri == null) {
            if (keystore == null)
                throw new IllegalArgumentException("require `--keystore` option (or `--uri`)");
            System.err.println((decrypt ? "decrypt" : "encrypt")
                + " with " + alias + " from " + storetype + " keystore " + keystore);
            System.out.println(decrypt ? cipher.decrypt(body(), config()) : cipher.encrypt(body(), config()));
        } else {
            if (decrypt)
                throw new IllegalArgumentException("can only encrypt when using --uri");
            if (!"https".equals(uri.getScheme()))
                throw new IllegalArgumentException("require 'https' scheme to get certificate from");
            System.err.println("encrypt for " + uri);
            Key key = loadCertificate(uri).getPublicKey();
            System.out.println(cipher.encrypt(body(), key));
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
