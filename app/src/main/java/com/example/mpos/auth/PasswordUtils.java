package com.example.mpos.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordUtils {
    private PasswordUtils() { }
    public static boolean verify(String plaintext, String hashed) {
        return hashed != null && hashed.equals(hash(plaintext));
    }

    public static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format("%02x", b));
            return result.toString();
        } catch (Exception exception) { throw new IllegalStateException("Cannot hash password", exception); }
    }
}
