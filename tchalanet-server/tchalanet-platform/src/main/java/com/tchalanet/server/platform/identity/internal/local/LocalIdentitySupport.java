package com.tchalanet.server.platform.identity.internal.local;

import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

final class LocalIdentitySupport {

  private static final Set<String> SAFE_CLAIMS =
      Set.of("preferred_username", "name", "phone_number");

  private final JwtDecoder jwtDecoder;
  private final LocalIdentityProperties properties;
  private final IdentityProviderType providerType;

  LocalIdentitySupport(
      JwtDecoder jwtDecoder, LocalIdentityProperties properties, IdentityProviderType providerType) {
    this.jwtDecoder = jwtDecoder;
    this.properties = properties;
    this.providerType = providerType;
  }

  ExternalAuthenticatedUser verify(String bearerToken) {
    if (bearerToken == null || bearerToken.isBlank()) {
      throw new IdentityProviderException("missing_token", "Bearer token is required", null);
    }
    try {
      var jwt = jwtDecoder.decode(bearerToken);
      return map(
          new VerifiedExternalToken(
              jwt.getClaimAsString("iss"),
              jwt.getSubject(),
              jwt.getClaimAsString("email"),
              Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
              jwt.getClaims()));
    } catch (JwtException | IllegalArgumentException ex) {
      throw new IdentityProviderException("invalid_token", "Local JWT verification failed", ex);
    }
  }

  ExternalAuthenticatedUser map(VerifiedExternalToken token) {
    if (!properties.requiredIssuer().equals(token.issuer())) {
      throw new IdentityProviderException("invalid_issuer", "Invalid local JWT issuer", null);
    }
    return new ExternalAuthenticatedUser(
        providerType,
        token.issuer(),
        token.subject(),
        token.email(),
        token.emailVerified(),
        safeClaims(token.verifiedClaims()));
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
