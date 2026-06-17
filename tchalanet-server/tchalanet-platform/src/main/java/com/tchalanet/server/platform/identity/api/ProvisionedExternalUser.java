package com.tchalanet.server.platform.identity.api;

import java.util.Objects;

public record ProvisionedExternalUser(
    IdentityProviderType provider,
    String issuer,
    String externalSubject,
    boolean created) {

  public ProvisionedExternalUser {
    Objects.requireNonNull(provider, "provider is required");
    issuer = requireText(issuer, "issuer");
    externalSubject = requireText(externalSubject, "externalSubject");
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value;
  }
}

