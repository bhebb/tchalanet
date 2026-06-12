package com.tchalanet.server.platform.identity.internal.local;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.identity.local")
public record LocalIdentityProperties(String issuer, String secret) {

  public String requiredIssuer() {
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalStateException("tch.identity.local.issuer is required");
    }
    return issuer.trim();
  }

  public String requiredSecret() {
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException(
          "tch.identity.local.secret must contain at least 32 characters");
    }
    return secret;
  }
}
