package com.tchalanet.server.platform.identity.internal.handoff;

import java.util.UUID;

interface ProviderSessionTokenIssuer {
  String createCustomToken(UUID appUserId);
}
