package com.tchalanet.server.platform.identity.api;

public interface IdentityProviderApi {

  ExternalAuthenticatedUser verifyBearerToken(
      String bearerToken, IdentityVerificationPolicy policy);

  ExternalAuthenticatedUser mapVerifiedToken(
      VerifiedExternalToken verifiedToken, IdentityVerificationPolicy policy);
}
