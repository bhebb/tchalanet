package com.tchalanet.server.platform.identity.internal.firebase;

import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderApi;
import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "tch.identity", name = "provider", havingValue = "firebase")
public final class FirebaseIdentityProvider implements IdentityProviderApi {

  private static final Set<String> SAFE_CLAIMS =
      Set.of("name", "picture", "phone_number", "firebase");

  private final FirebaseTokenVerifier tokenVerifier;
  private final FirebaseIdentityProperties properties;
  private final FirebaseRevocationChecker revocationChecker;

  public FirebaseIdentityProvider(
      FirebaseTokenVerifier tokenVerifier,
      FirebaseIdentityProperties properties,
      FirebaseRevocationChecker revocationChecker) {
    this.tokenVerifier = tokenVerifier;
    this.properties = properties;
    this.revocationChecker = revocationChecker;
  }

  @Override
  public ExternalAuthenticatedUser verifyBearerToken(
      String bearerToken, IdentityVerificationPolicy policy) {
    Objects.requireNonNull(policy, "verification policy is required");
    if (bearerToken == null || bearerToken.isBlank()) {
      throw new IdentityProviderException("missing_token", "Bearer token is required", null);
    }
    try {
      var jwt = tokenVerifier.verify(bearerToken);
      return mapVerifiedToken(
          new VerifiedExternalToken(
              jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
              jwt.getSubject(),
              jwt.getClaimAsString("email"),
              Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
              jwt.getClaims()),
          policy);
    } catch (JwtException | IllegalArgumentException ex) {
      throw new IdentityProviderException(
          "invalid_token", "Firebase ID token verification failed", ex);
    }
  }

  @Override
  public ExternalAuthenticatedUser mapVerifiedToken(
      VerifiedExternalToken verifiedToken, IdentityVerificationPolicy policy) {
    Objects.requireNonNull(verifiedToken, "verified token is required");
    Objects.requireNonNull(policy, "verification policy is required");
    validateFirebaseClaims(verifiedToken);
    if (properties.effectiveRevocationCheckMode().requiresCheck(policy)) {
      revocationChecker.check(verifiedToken);
    }
    return new ExternalAuthenticatedUser(
        IdentityProviderType.FIREBASE,
        verifiedToken.issuer(),
        verifiedToken.subject(),
        verifiedToken.email(),
        verifiedToken.emailVerified(),
        safeClaims(verifiedToken.verifiedClaims()));
  }

  private void validateFirebaseClaims(VerifiedExternalToken token) {
    if (!properties.issuer().equals(token.issuer())) {
      throw new IdentityProviderException("invalid_issuer", "Invalid Firebase token issuer", null);
    }
    if (token.subject().length() > 128) {
      throw new IdentityProviderException("invalid_subject", "Invalid Firebase token subject", null);
    }
    var audience = audience(token.verifiedClaims().get("aud"));
    if (!audience.contains(properties.requiredProjectId())) {
      throw new IdentityProviderException("invalid_audience", "Invalid Firebase token audience", null);
    }
  }

  private static List<String> audience(Object claim) {
    if (claim instanceof String value) {
      return List.of(value);
    }
    if (claim instanceof List<?> values) {
      return values.stream().map(Object::toString).toList();
    }
    return List.of();
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
