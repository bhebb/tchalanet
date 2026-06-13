package com.tchalanet.server.platform.identity.internal.firebase;

import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "tch.identity",
    name = "provider",
    havingValue = "firebase-emulator")
final class FirebaseEmulatorRevocationChecker implements FirebaseRevocationChecker {

  @Override
  public void check(VerifiedExternalToken token) {
    // The local emulator has no production revocation/disabled-user guarantees.
  }
}
