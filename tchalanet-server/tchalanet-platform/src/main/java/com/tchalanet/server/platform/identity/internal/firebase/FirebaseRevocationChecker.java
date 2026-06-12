package com.tchalanet.server.platform.identity.internal.firebase;

import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;

interface FirebaseRevocationChecker {

  void check(VerifiedExternalToken token);
}
