package com.tchalanet.server.platform.identity.api;

import java.util.Map;
import java.util.Objects;

public record ExternalAuthenticatedUser(
    IdentityProviderType provider,
    String issuer,
    String subject,
    String email,
    boolean emailVerified,
    Map<String, Object> safeClaims) {

  public ExternalAuthenticatedUser {
    Objects.requireNonNull(provider, "provider is required");
    issuer = requireText(issuer, "issuer");
    subject = requireText(subject, "subject");
    safeClaims = safeClaims == null ? Map.of() : Map.copyOf(safeClaims);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value;
  }
}

