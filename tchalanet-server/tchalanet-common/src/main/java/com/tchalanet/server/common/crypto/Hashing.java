package com.tchalanet.server.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small crypto helper for stable hashing operations used across the project.
 *
 * <p>This class is for deterministic technical hashes such as request hashing,
 * idempotency payload hashes, cache keys, or integrity fingerprints.
 *
 * <p>Do not use this for password hashing.
 */
public final class Hashing {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Hashing() {}

    /**
     * Computes the SHA-256 hex digest of the provided string using UTF-8.
     *
     * <p>If the input is null, the digest of the empty string is returned.
     */
    public static String sha256Hex(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));

            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        var out = new char[bytes.length * 2];

        for (int i = 0, j = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[j++] = HEX[v >>> 4];
            out[j++] = HEX[v & 0x0F];
        }

        return new String(out);
    }
}
