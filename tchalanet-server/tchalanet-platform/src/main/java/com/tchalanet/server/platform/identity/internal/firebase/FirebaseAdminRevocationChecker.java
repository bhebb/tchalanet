package com.tchalanet.server.platform.identity.internal.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "tch.identity", name = "provider", havingValue = "firebase")
final class FirebaseAdminRevocationChecker implements FirebaseRevocationChecker {

  private final FirebaseAuth firebaseAuth;

  FirebaseAdminRevocationChecker(FirebaseAuth firebaseAuth) {
    this.firebaseAuth = firebaseAuth;
  }

  @Override
  public void check(VerifiedExternalToken token) {
    try {
      var user = firebaseAuth.getUser(token.subject());
      if (user.isDisabled()) {
        throw new IdentityProviderException(
            "firebase_user_disabled", "Firebase user is disabled", null);
      }
      if (authenticationTime(token).toEpochMilli() < user.getTokensValidAfterTimestamp()) {
        throw new IdentityProviderException(
            "firebase_token_revoked", "Firebase ID token has been revoked", null);
      }
    } catch (FirebaseAuthException ex) {
      throw new IdentityProviderException(
          "firebase_revocation_check_failed", "Firebase user status verification failed", ex);
    }
  }

  private static Instant authenticationTime(VerifiedExternalToken token) {
    var authTime = token.verifiedClaims().get("auth_time");
    if (authTime instanceof Instant instant) {
      return instant;
    }
    if (authTime instanceof Number number) {
      return Instant.ofEpochSecond(number.longValue());
    }
    throw new IdentityProviderException(
        "invalid_auth_time", "Firebase ID token auth_time is required", null);
  }
}
