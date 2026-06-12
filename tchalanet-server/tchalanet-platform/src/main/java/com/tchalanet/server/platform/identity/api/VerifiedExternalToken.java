package com.tchalanet.server.platform.identity.api;

import java.util.Map;
import java.util.Objects;

/**
 * Provider-neutral facts extracted from a token that has already passed cryptographic and standard
 * claim verification.
 */
public record VerifiedExternalToken(
    String issuer,
    String subject,
    String email,
    boolean emailVerified,
    Map<String, Object> verifiedClaims) {

  public VerifiedExternalToken {
    issuer = requireText(issuer, "issuer");
    subject = requireText(subject, "subject");
    verifiedClaims = Map.copyOf(Objects.requireNonNull(verifiedClaims, "verifiedClaims is required"));
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value;
  }
}

