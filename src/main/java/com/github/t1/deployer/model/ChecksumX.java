package com.github.t1.deployer.model;

import lombok.*;

import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;

import static javax.xml.bind.DatatypeConverter.*;
import static lombok.AccessLevel.*;

@Value
@RequiredArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@XmlAccessorType(XmlAccessType.NONE)
public class ChecksumX {
    public static ChecksumX of(byte[] bytes) {
        return new ChecksumX(bytes);
    }

    public static ChecksumX fromString(String hexString) {
        return ofHexString(hexString);
    }

    public static ChecksumX ofHexString(String hexString) {
        return of(parseHexBinary(hexString));
    }

    public static ChecksumX ofBase64(String hexString) {
        return of(parseBase64Binary(hexString));
    }

    public static ChecksumX sha1(Path path) {
        return of(path, "SHA-1");
    }

    public static ChecksumX md5(Path path) {
        return of(path, "MD5");
    }

    @SneakyThrows(IOException.class)
    private static ChecksumX of(Path path, String algorithm) {
        return of(Files.readAllBytes(path), algorithm);
    }

    public static ChecksumX sha1(byte[] bytes) {
        return of(bytes, "SHA-1");
    }

    public static ChecksumX md5(byte[] bytes) {
        return of(bytes, "MD5");
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private static ChecksumX of(byte[] bytes, String algorithm) {
        MessageDigest hash = MessageDigest.getInstance(algorithm);
        return of(hash.digest(bytes));
    }

    @NonNull
    @XmlValue
    @XmlSchemaType(name = "hexBinary")
    private final byte[] bytes;

    public String hexString() {
        return printHexBinary(bytes);
    }

    public String base64() {
        return printBase64Binary(bytes);
    }

    public String hexByteArray() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i != 0)
                out.append(", ");
            out.append("0x").append(printHexBinary(new byte[] { bytes[i] }));
        }
        return out.toString();
    }

    public boolean isEmpty() {
        return bytes.length == 0;
    }

    @Override
    public String toString() {
        return hexString().toLowerCase();
    }
}
