package com.tchalanet.server.platform.identity.internal.handoff;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.error.IdentityErrorCodes;
import java.util.UUID;
import org.springframework.stereotype.Component;

// Always registered so a ProviderSessionTokenIssuer bean exists for every identity provider
// (local-jwt, firebase-emulator, …). When Firebase is active its @Primary issuer wins; otherwise
// this fallback is the sole bean. (@ConditionalOnMissingBean on a @Component is unreliable.)
@Component
class UnsupportedProviderSessionTokenIssuer implements ProviderSessionTokenIssuer {
  @Override
  public String createCustomToken(UUID appUserId) {
    throw ProblemRest.of(IdentityErrorCodes.HANDOFF_CUSTOM_TOKEN_UNSUPPORTED);
  }
}
