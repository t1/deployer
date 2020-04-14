package com.github.t1.deployer.model;

import com.github.t1.deployer.model.Checksum.ChecksumDeserializer;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;

import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlValue;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static javax.xml.bind.DatatypeConverter.printHexBinary;
import static lombok.AccessLevel.PRIVATE;

@Value
@RequiredArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@XmlAccessorType(XmlAccessType.NONE)
@JsonbTypeDeserializer(ChecksumDeserializer.class)
public class Checksum {
    public static Checksum of(byte[] bytes) { return new Checksum(bytes); }

    public static Checksum fromString(String hexString) { return ofHexString(hexString); }

    public static Checksum ofHexString(String hexString) { return of(parseHexBinary(hexString)); }

    public static Checksum sha1(Path path) { return of(path, "SHA-1"); }

    public static Checksum md5(Path path) { return of(path, "MD5"); }

    @SneakyThrows(IOException.class)
    private static Checksum of(Path path, String algorithm) { return of(Files.readAllBytes(path), algorithm); }

    public static Checksum sha1(byte[] bytes) { return of(bytes, "SHA-1"); }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private static Checksum of(byte[] bytes, String algorithm) {
        MessageDigest hash = MessageDigest.getInstance(algorithm);
        return of(hash.digest(bytes));
    }

    @NonNull
    @XmlValue
    @XmlSchemaType(name = "hexBinary")
    byte[] bytes;

    public String hexString() { return printHexBinary(bytes); }

    public String hexByteArray() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i != 0)
                out.append(", ");
            out.append("0x").append(printHexBinary(new byte[]{bytes[i]}));
        }
        return out.toString();
    }

    public boolean isEmpty() { return bytes.length == 0; }

    @Override public String toString() { return hexString().toLowerCase(); }

    public static class ChecksumDeserializer implements JsonbDeserializer<Checksum> {
        @Override public Checksum deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return Checksum.ofHexString(parser.getString());
        }
    }
}
