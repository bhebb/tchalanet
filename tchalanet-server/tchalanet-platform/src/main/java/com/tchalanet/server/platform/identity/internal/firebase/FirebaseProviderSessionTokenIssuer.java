package com.tchalanet.server.platform.identity.internal.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.error.IdentityErrorCodes;
import com.tchalanet.server.platform.identity.internal.handoff.ProviderSessionTokenIssuer;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserExternalIdentityJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(FirebaseAuth.class)
@Primary
@RequiredArgsConstructor
class FirebaseProviderSessionTokenIssuer implements ProviderSessionTokenIssuer {

  private final FirebaseAuth firebaseAuth;
  private final AppUserExternalIdentityJpaRepository externalIdentities;

  @Override
  public String createCustomToken(UUID appUserId) {
    var externalSubject =
        externalIdentities
            .findFirstByAppUserIdAndProvider(appUserId, IdentityProviderType.FIREBASE)
            .orElseThrow(() -> ProblemRest.of(IdentityErrorCodes.HANDOFF_EXTERNAL_IDENTITY_MISSING))
            .getExternalSubject();
    try {
      return firebaseAuth.createCustomToken(externalSubject);
    } catch (FirebaseAuthException ex) {
      throw ProblemRest.of(IdentityErrorCodes.HANDOFF_CUSTOM_TOKEN_FAILED, java.util.Map.of(), ex);
    }
  }
}
