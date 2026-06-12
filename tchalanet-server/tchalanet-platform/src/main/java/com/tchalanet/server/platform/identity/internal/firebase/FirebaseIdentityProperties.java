package com.tchalanet.server.platform.identity.internal.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.identity.firebase")
public record FirebaseIdentityProperties(
    String projectId,
    String jwksUri,
    String credentialsPath,
    FirebaseRevocationCheckMode revocationCheckMode) {

  public static final String DEFAULT_JWKS_URI =
      "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

  public String requiredProjectId() {
    if (projectId == null || projectId.isBlank()) {
      throw new IllegalStateException("tch.identity.firebase.project-id is required");
    }
    return projectId.trim();
  }

  public String issuer() {
    return "https://securetoken.google.com/" + requiredProjectId();
  }

  public String effectiveJwksUri() {
    return jwksUri == null || jwksUri.isBlank() ? DEFAULT_JWKS_URI : jwksUri.trim();
  }

  public FirebaseRevocationCheckMode effectiveRevocationCheckMode() {
    return revocationCheckMode == null
        ? FirebaseRevocationCheckMode.SENSITIVE_ONLY
        : revocationCheckMode;
  }
}
