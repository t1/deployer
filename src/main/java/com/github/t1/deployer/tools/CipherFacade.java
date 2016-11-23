package com.github.t1.deployer.tools;

import com.beust.jcommander.*;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.security.*;
import java.security.KeyStore.*;
import java.security.cert.Certificate;
import java.util.List;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.*;
import static javax.crypto.Cipher.*;
import static javax.xml.bind.DatatypeConverter.*;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CipherFacade {
    private static final String DEFAULT_PASS = "changeit";

    public static void main(String... args) { System.out.println(new CipherFacade(args).run()); }

    public CipherFacade(String[] args) {
        JCommander cli = new JCommander(this, args);
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
    private String alias = "secretkey";
    @Parameter(names = "--decrypt", description = "Decrypt instead of encrypt")
    private boolean decrypt = false;
    @Parameter(names = "--uri",
               description = "Use the certificate of a 'https' server to encrypt. Either this or `--keystore` is mandatory.")
    private URI uri;

    private String run() {
        if (uri == null) {
            if (keystore == null)
                throw new IllegalArgumentException("require `--keystore` option (or `--uri`)");
            System.err.println((decrypt ? "decrypt" : "encrypt")
                    + " with " + alias + " from " + storetype + " keystore " + keystore);
            return decrypt ? decrypt(body(), config()) : encrypt(body(), config());
        } else {
            if (decrypt)
                throw new IllegalArgumentException("can only encrypt when using --uri");
            if (!"https".equals(uri.getScheme()))
                throw new IllegalArgumentException("require 'https' scheme to get certificate from");
            System.err.println("encrypt for " + uri);
            Key key = loadCertificate(uri).getPublicKey();
            return encrypt(body(), key);
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
        return KeyStoreConfig
                .builder()
                .path(Paths.get(keystore))
                .type(storetype)
                .pass(storepass)
                .alias(alias)
                .build();
    }

    public static String encrypt(String plain, KeyStoreConfig config) {
        Key key = loadKey(config, entry -> entry.getCertificate().getPublicKey());
        return encrypt(plain, key);
    }

    private static String encrypt(String plain, Key key) {
        return printHexBinary(cipher(ENCRYPT_MODE, plain.getBytes(UTF_8), key));
    }

    public static String decrypt(String text, KeyStoreConfig config) {
        Key key = loadKey(config, PrivateKeyEntry::getPrivateKey);
        return new String(cipher(DECRYPT_MODE, parseHexBinary(text), key), UTF_8);
    }

    @SneakyThrows({ GeneralSecurityException.class, IOException.class })
    private static Key loadKey(KeyStoreConfig config, Function<PrivateKeyEntry, Key> privateKeyExtractor) {
        KeyStore store = loadKeyStore(config);

        if (store.isCertificateEntry(config.getAlias()))
            return store.getCertificate(config.getAlias()).getPublicKey();
        Entry entry = store.getEntry(config.getAlias(), new PasswordProtection(getKeyPass(config)));
        if (entry == null)
            throw new IllegalArgumentException("no key [" + config.getAlias() + "] in " + getKeyStorePath(config));
        return (entry instanceof PrivateKeyEntry)
                ? privateKeyExtractor.apply((PrivateKeyEntry) entry)
                : ((SecretKeyEntry) entry).getSecretKey();
    }

    @NotNull private static KeyStore loadKeyStore(KeyStoreConfig config) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance(getKeystoreType(config));
        store.load(Files.newInputStream(getKeyStorePath(config)), getKeyPass(config));
        return store;
    }

    @SneakyThrows(GeneralSecurityException.class)
    private static byte[] cipher(int mode, byte[] bytes, Key key) {
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(mode, key);
        return cipher.doFinal(bytes);
    }

    private static Path getKeyStorePath(KeyStoreConfig keyStore) {
        if (keyStore == null)
            throw new RuntimeException("no key-store configured to decrypt expression");
        return keyStore.getPath();
    }

    private static char[] getKeyPass(KeyStoreConfig keyStore) {
        return ((keyStore == null || keyStore.getPass() == null) ? DEFAULT_PASS : keyStore.getPass()).toCharArray();
    }

    private static String getKeystoreType(KeyStoreConfig keyStore) {
        return (keyStore == null || keyStore.getType() == null) ? KeyStore.getDefaultType() : keyStore.getType();
    }
}
