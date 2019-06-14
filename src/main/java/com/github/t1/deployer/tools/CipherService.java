package com.github.t1.deployer.tools;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.SecretKeyEntry;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static javax.xml.bind.DatatypeConverter.printHexBinary;

public class CipherService {
    public static final String DEFAULT_PASS = "changeit";

    public String encrypt(String plain, KeyStoreConfig config) {
        Key key = loadKey(config, entry -> entry.getCertificate().getPublicKey());
        return encrypt(plain, key);
    }

    public String encrypt(String plain, Key key) {
        return printHexBinary(cipher(ENCRYPT_MODE, plain.getBytes(UTF_8), key));
    }

    public String decrypt(String text, KeyStoreConfig config) {
        Key key = loadKey(config, PrivateKeyEntry::getPrivateKey);
        return new String(cipher(DECRYPT_MODE, parseHexBinary(text), key), UTF_8);
    }

    @SneakyThrows({GeneralSecurityException.class, IOException.class})
    private Key loadKey(KeyStoreConfig config, Function<PrivateKeyEntry, Key> privateKeyExtractor) {
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

    @NotNull private KeyStore loadKeyStore(KeyStoreConfig config) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance(getKeystoreType(config));
        try (InputStream inputStream = Files.newInputStream(getKeyStorePath(config))) {
            store.load(inputStream, getKeyPass(config));
        }
        return store;
    }

    @SneakyThrows(GeneralSecurityException.class)
    private byte[] cipher(int mode, byte[] bytes, Key key) {
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(mode, key);
        return cipher.doFinal(bytes);
    }

    private Path getKeyStorePath(KeyStoreConfig keyStore) {
        if (keyStore == null)
            throw new RuntimeException("no key-store configured");
        if (keyStore.getPath() == null)
            throw new RuntimeException("no key-store path configured");
        return Paths.get(keyStore.getPath());
    }

    private char[] getKeyPass(KeyStoreConfig keyStore) {
        return ((keyStore == null || keyStore.getPass() == null) ? DEFAULT_PASS : keyStore.getPass()).toCharArray();
    }

    private String getKeystoreType(KeyStoreConfig keyStore) {
        return (keyStore == null || keyStore.getType() == null) ? KeyStore.getDefaultType() : keyStore.getType();
    }
}
