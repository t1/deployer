package com.github.t1.deployer.tools;

import com.beust.jcommander.*;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.security.KeyStore.*;
import java.util.List;

import static java.nio.charset.StandardCharsets.*;
import static javax.crypto.Cipher.*;
import static javax.xml.bind.DatatypeConverter.*;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CipherFacade {

    public static void main(String... args) {
        System.out.println(new CipherFacade(args).run());
    }

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
    @Parameter(names = "--storepass") private String storepass;
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
        return printHexBinary(cipher(ENCRYPT_MODE, config, plain.getBytes(UTF_8)));
    }

    public static String decrypt(String text, KeyStoreConfig keyStore) {
        return new String(cipher(DECRYPT_MODE, keyStore, parseHexBinary(text)), UTF_8);
    }

    @SneakyThrows({ GeneralSecurityException.class, IOException.class })
    private static byte[] cipher(int mode, KeyStoreConfig keyStore, byte[] bytes) {
        Key key = loadKey(keyStore);
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(mode, key);
        return cipher.doFinal(bytes);
    }

    private static Key loadKey(KeyStoreConfig keyStore) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance(getKeystoreType(keyStore));
        char[] storePass = getKeyPass(keyStore);
        store.load(Files.newInputStream(getKeyStorePath(keyStore)), storePass);
        PasswordProtection protection = new PasswordProtection(storePass);
        Entry entry = store.getEntry(keyStore.getAlias(), protection);
        return (entry instanceof PrivateKeyEntry)
                ? ((PrivateKeyEntry) entry).getPrivateKey()
                : ((SecretKeyEntry) entry).getSecretKey();
    }

    private static Path getKeyStorePath(KeyStoreConfig keyStore) {
        if (keyStore == null)
            throw new RuntimeException("no key-store configured to decrypt expression");
        return keyStore.getPath();
    }

    private static char[] getKeyPass(KeyStoreConfig keyStore) {
        return ((keyStore == null || keyStore.getPass() == null) ? "changeit" : keyStore.getPass()).toCharArray();
    }

    private static String getKeystoreType(KeyStoreConfig keyStore) {
        return (keyStore == null || keyStore.getType() == null) ? KeyStore.getDefaultType() : keyStore.getType();
    }
}
