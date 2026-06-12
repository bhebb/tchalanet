package com.tchalanet.server.platform.identity.internal.local;

import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderApi;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "tch.identity", name = "provider", havingValue = "local-jwt")
public final class LocalJwtIdentityProvider implements IdentityProviderApi {

  private final LocalIdentitySupport support;

  public LocalJwtIdentityProvider(JwtDecoder jwtDecoder, LocalIdentityProperties properties) {
    this.support = new LocalIdentitySupport(jwtDecoder, properties, IdentityProviderType.LOCAL_JWT);
  }

  @Override
  public ExternalAuthenticatedUser verifyBearerToken(
      String bearerToken, IdentityVerificationPolicy policy) {
    Objects.requireNonNull(policy, "verification policy is required");
    return support.verify(bearerToken);
  }

  @Override
  public ExternalAuthenticatedUser mapVerifiedToken(
      VerifiedExternalToken verifiedToken, IdentityVerificationPolicy policy) {
    Objects.requireNonNull(policy, "verification policy is required");
    return support.map(Objects.requireNonNull(verifiedToken, "verified token is required"));
  }
}
