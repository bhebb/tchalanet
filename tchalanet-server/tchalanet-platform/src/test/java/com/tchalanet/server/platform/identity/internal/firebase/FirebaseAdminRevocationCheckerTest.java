package com.tchalanet.server.platform.identity.internal.firebase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FirebaseAdminRevocationCheckerTest {

  private final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
  private final UserRecord user = Mockito.mock(UserRecord.class);
  private final FirebaseAdminRevocationChecker checker =
      new FirebaseAdminRevocationChecker(firebaseAuth);

  @Test
  void rejectsDisabledFirebaseUser() throws Exception {
    when(firebaseAuth.getUser("firebase-uid")).thenReturn(user);
    when(user.isDisabled()).thenReturn(true);

    assertRejected(token(1_750_000_000L), "firebase_user_disabled");
  }

  @Test
  void rejectsTokenAuthenticatedBeforeTokensValidAfterTimestamp() throws Exception {
    when(firebaseAuth.getUser("firebase-uid")).thenReturn(user);
    when(user.getTokensValidAfterTimestamp()).thenReturn(1_750_000_001_000L);

    assertRejected(token(1_750_000_000L), "firebase_token_revoked");
  }

  @Test
  void rejectsMissingAuthenticationTimeWhenRemoteCheckIsRequired() throws Exception {
    when(firebaseAuth.getUser("firebase-uid")).thenReturn(user);

    assertRejected(
        new VerifiedExternalToken(
            "issuer", "firebase-uid", null, false, Map.of("aud", "project")),
        "invalid_auth_time");
  }

  @Test
  void failsClosedWhenFirebaseUserStatusCannotBeVerified() throws Exception {
    when(firebaseAuth.getUser("firebase-uid"))
        .thenThrow(Mockito.mock(FirebaseAuthException.class));

    assertRejected(token(1_750_000_000L), "firebase_revocation_check_failed");
  }

  private void assertRejected(VerifiedExternalToken token, String code) {
    assertThatThrownBy(() -> checker.check(token))
        .isInstanceOf(IdentityProviderException.class)
        .extracting("code")
        .isEqualTo(code);
  }

  private static VerifiedExternalToken token(long authenticationTimeSeconds) {
    return new VerifiedExternalToken(
        "issuer",
        "firebase-uid",
        null,
        false,
        Map.of("aud", "project", "auth_time", authenticationTimeSeconds));
  }
}
