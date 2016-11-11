package com.github.t1.deployer.tools;

import com.beust.jcommander.*;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.security.KeyStore.*;
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

    @Parameter(names = "--help", help = true) private boolean help;

    @Parameter private List<String> bodies;
    @Parameter(names = "--keystore") private String keystore;
    @Parameter(names = "--storetype") private String storetype = KeyStore.getDefaultType();
    @Parameter(names = "--storepass") private String storepass = DEFAULT_PASS;
    @Parameter(names = "--alias") private String alias = "secretkey";
    @Parameter(names = "--decrypt") private boolean decrypt = false;

    private String run() {
        System.err.println(
                (decrypt ? "decrypt" : "encrypt") + " with " + alias + " from " + storetype + " keystore " + keystore);
        return decrypt ? decrypt(body(), config()) : encrypt(body(), config());
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
