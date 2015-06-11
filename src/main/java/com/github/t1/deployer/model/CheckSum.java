package com.github.t1.deployer.model;

import static javax.xml.bind.DatatypeConverter.*;
import static javax.xml.bind.annotation.XmlAccessType.*;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;

import javax.xml.bind.annotation.*;

import lombok.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Value
@XmlAccessorType(NONE)
@RequiredArgsConstructor
@JsonSerialize(using = ToStringSerializer.class)
public class CheckSum {
    public static CheckSum of(byte[] bytes) {
        return new CheckSum(bytes);
    }

    public static CheckSum fromString(String hexString) {
        return ofHexString(hexString);
    }

    public static CheckSum ofHexString(String hexString) {
        return of(parseHexBinary(hexString));
    }

    public static CheckSum ofBase64(String hexString) {
        return of(parseBase64Binary(hexString));
    }

    public static CheckSum sha1(Path path) {
        return of(path, "SHA-1");
    }

    public static CheckSum md5(Path path) {
        return of(path, "MD5");
    }

    @SneakyThrows(IOException.class)
    public static CheckSum of(Path path, String algorithm) {
        return of(Files.readAllBytes(path), algorithm);
    }

    public static CheckSum sha1(byte[] bytes) {
        return of(bytes, "SHA-1");
    }

    public static CheckSum md5(byte[] bytes) {
        return of(bytes, "MD5");
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    public static CheckSum of(byte[] bytes, String algorithm) {
        MessageDigest hash = MessageDigest.getInstance(algorithm);
        return of(hash.digest(bytes));
    }

    @NonNull
    @XmlValue
    @XmlSchemaType(name = "hexBinary")
    private final byte[] bytes;

    /** required by JAXB */
    @SuppressWarnings("unused")
    private CheckSum() {
        this.bytes = null;
    }

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
        return hexString();
    }
}
