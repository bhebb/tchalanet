package com.tchalanet.server.core.address.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Factory for generating deduplication keys (normalized hash).
 * Per spec: SHA-256 hex of normalized address.
 */
public class AddressDedupeKeyFactory {

  /**
   * Generate SHA-256 hex from normalized address string.
   *
   * @param normalizedAddress output of AddressNormalizer.normalize()
   * @return 64-char hex string (SHA-256)
   */
  public static String generateKey(String normalizedAddress) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encoded = digest.digest(normalizedAddress.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(encoded);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 should always be available
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : bytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }

  /**
   * Convenience: normalize + generate key in one step.
   */
  public static String generateKeyFromFields(String line1, String line2, String city, String region, String country, String postalCode) {
    String normalized = AddressNormalizer.normalize(line1, line2, city, region, country, postalCode);
    return generateKey(normalized);
  }
}
