package com.tchalanet.server.platform.identity.internal.keycloak;

import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderApi;
import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "tch.identity",
    name = "provider",
    havingValue = "keycloak",
    matchIfMissing = true)
public final class KeycloakIdentityProvider implements IdentityProviderApi {

  private static final Set<String> SAFE_CLAIMS =
      Set.of("preferred_username", "name", "given_name", "family_name", "phone_number");

  private final JwtDecoder jwtDecoder;

  public KeycloakIdentityProvider(JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
  }

  @Override
  public ExternalAuthenticatedUser verifyBearerToken(
      String bearerToken, IdentityVerificationPolicy policy) {
    Objects.requireNonNull(policy, "verification policy is required");
    if (bearerToken == null || bearerToken.isBlank()) {
      throw new IdentityProviderException("missing_token", "Bearer token is required", null);
    }

    try {
      var jwt = jwtDecoder.decode(bearerToken);
      return mapVerifiedToken(
          new VerifiedExternalToken(
              issuer(jwt),
              jwt.getSubject(),
              jwt.getClaimAsString("email"),
              Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
              jwt.getClaims()),
          policy);
    } catch (JwtException | IllegalArgumentException ex) {
      throw new IdentityProviderException(
          "invalid_token", "External identity token verification failed", ex);
    }
  }

  @Override
  public ExternalAuthenticatedUser mapVerifiedToken(
      VerifiedExternalToken verifiedToken, IdentityVerificationPolicy policy) {
    Objects.requireNonNull(verifiedToken, "verified token is required");
    Objects.requireNonNull(policy, "verification policy is required");
    return new ExternalAuthenticatedUser(
        IdentityProviderType.KEYCLOAK,
        verifiedToken.issuer(),
        verifiedToken.subject(),
        verifiedToken.email(),
        verifiedToken.emailVerified(),
        safeClaims(verifiedToken.verifiedClaims()));
  }

  private static String issuer(Jwt jwt) {
    return jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
  }

  private static Map<String, Object> safeClaims(Map<String, Object> verifiedClaims) {
    var claims = new LinkedHashMap<String, Object>();
    SAFE_CLAIMS.forEach(
        claim -> {
          var value = verifiedClaims.get(claim);
          if (value != null) {
            claims.put(claim, value);
          }
        });
    return claims;
  }
}
