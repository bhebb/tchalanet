package com.tchalanet.server.core.address.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Normalizes address fields for deduplication.
 * Per spec: trim, lowercase, collapse spaces, remove weak punctuation.
 */
public class AddressNormalizer {

  /**
   * Normalize address fields for comparison.
   * Returns a canonical string suitable for hashing.
   *
   * Fields: line1, line2, city, region, country, postalCode
   */
  public static String normalize(String line1, String line2, String city, String region, String country, String postalCode) {
    StringBuilder sb = new StringBuilder();

    sb.append(normalizeField(line1));
    if (line2 != null && !line2.isBlank()) {
      sb.append("|").append(normalizeField(line2));
    }
    sb.append("|").append(normalizeField(city));
    if (region != null && !region.isBlank()) {
      sb.append("|").append(normalizeField(region));
    }
    sb.append("|").append(normalizeField(country));
    sb.append("|").append(normalizePostalCode(postalCode));

    return sb.toString();
  }

  /**
   * Normalize a single field: trim, lowercase, collapse spaces, remove weak punctuation.
   */
  private static String normalizeField(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    // trim + lowercase
    String normalized = value.trim().toLowerCase();
    // collapse multiple spaces
    normalized = normalized.replaceAll("\\s+", " ");
    // remove weak punctuation: , . - # ' "
    normalized = normalized.replaceAll("[,.\\-#'\"]", "");
    return normalized;
  }

  /**
   * Normalize postal code: uppercase, remove spaces.
   */
  private static String normalizePostalCode(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim().toUpperCase().replaceAll("\\s+", "");
  }
}
