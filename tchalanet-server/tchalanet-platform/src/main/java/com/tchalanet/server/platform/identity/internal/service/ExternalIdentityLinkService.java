package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserExternalIdentityJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserExternalIdentityJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalIdentityLinkService {

  private final AppUserExternalIdentityJpaRepository externalIdentities;

  public void link(
      UserId appUserId,
      IdentityProviderType provider,
      String issuer,
      String externalSubject,
      String emailSnapshot) {
    var existing =
        externalIdentities.findByProviderAndIssuerAndExternalSubject(
            provider, issuer, externalSubject);
    if (existing.isPresent() && !existing.orElseThrow().getAppUserId().equals(appUserId.value())) {
      throw new IllegalStateException("External identity is already linked to another AppUser");
    }
    var identity = existing.orElseGet(AppUserExternalIdentityJpaEntity::new);
    identity.setAppUserId(appUserId.value());
    identity.setProvider(provider);
    identity.setIssuer(issuer);
    identity.setExternalSubject(externalSubject);
    identity.setEmailSnapshot(emailSnapshot);
    externalIdentities.save(identity);
  }
}
