package com.github.t1.deployer;

import static javax.xml.bind.DatatypeConverter.*;
import static javax.xml.bind.annotation.XmlAccessType.*;

import javax.xml.bind.annotation.*;

import lombok.*;

@Value
@XmlAccessorType(NONE)
@org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.codehaus.jackson.map.annotate.JsonSerialize(using = org.codehaus.jackson.map.ser.std.ToStringSerializer.class,
        include = org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL)
@com.fasterxml.jackson.databind.annotation.JsonSerialize(
        using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@RequiredArgsConstructor
public class CheckSum {
    @com.fasterxml.jackson.annotation.JsonCreator
    @org.codehaus.jackson.annotate.JsonCreator
    public static CheckSum fromString(String hexString) {
        return ofHexString(hexString);
    }

    public static CheckSum ofHexString(String hexString) {
        return of(parseHexBinary(hexString));
    }

    public static CheckSum ofBase64(String hexString) {
        return of(parseBase64Binary(hexString));
    }

    public static CheckSum of(byte[] bytes) {
        return new CheckSum(bytes);
    }

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

    @Override
    public String toString() {
        return hexString();
    }
}
