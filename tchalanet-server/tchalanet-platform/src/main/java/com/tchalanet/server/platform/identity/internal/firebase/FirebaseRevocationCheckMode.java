package com.tchalanet.server.platform.identity.internal.firebase;

import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;

enum FirebaseRevocationCheckMode {
  OFF,
  SENSITIVE_ONLY,
  ALWAYS;

  boolean requiresCheck(IdentityVerificationPolicy policy) {
    return this == ALWAYS || (this == SENSITIVE_ONLY && policy == IdentityVerificationPolicy.SENSITIVE);
  }
}
