package com.tchalanet.server.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Small crypto helper for stable hashing operations used across the project. Provides a single
 * sha256Hex utility so we don't duplicate the implementation.
 */
public final class Hashing {

  private Hashing() {}

  /**
   * Compute the SHA-256 hex digest of the provided string (UTF-8). If the input is null, the digest
   * of the empty string is returned.
   *
   * @param s input string or null
   * @return lower-case hex string representation of SHA-256 (64 chars)
   * @throws IllegalStateException if SHA-256 algorithm is not available (very unlikely)
   */
  public static String sha256Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(dig.length * 2);
      for (byte b : dig) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException("sha256 failed", e);
    }
  }
}
