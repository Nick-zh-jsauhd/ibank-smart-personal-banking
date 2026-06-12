package com.bank.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] derived = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.trim().length() == 0) {
            return false;
        }
        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            return false;
        }
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expected = Base64.getDecoder().decode(parts[2]);
        byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations, expected.length * 8);
        return constantTimeEquals(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Password hash algorithm is not available", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid password hash specification", e);
        }
    }

    private static boolean constantTimeEquals(byte[] left, byte[] right) {
        if (left.length != right.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < left.length; i++) {
            diff |= left[i] ^ right[i];
        }
        return diff == 0;
    }
}
