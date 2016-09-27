package com.github.t1.deployer.testtools;

import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.security.*;

import static java.nio.charset.StandardCharsets.*;

@Log
public class UserHashTool {
    private static final String REALM = "ManagementRealm";

    public static void main(String[] args) {
        String userName = args[0];
        String password = args[1];
        log.info("hash: " + hex(md5(userName + ":" + REALM + ":" + password)));
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private static byte[] md5(String input) {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(input.getBytes(UTF_8));
    }

    private static String hex(byte[] bytes) {
        return String.format("%x", new BigInteger(1, bytes));
    }
}
