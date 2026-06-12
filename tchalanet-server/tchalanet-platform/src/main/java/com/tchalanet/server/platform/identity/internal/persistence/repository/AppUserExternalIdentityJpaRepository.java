package com.tchalanet.server.platform.identity.internal.persistence.repository;

import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserExternalIdentityJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserExternalIdentityJpaRepository
    extends JpaRepository<AppUserExternalIdentityJpaEntity, UUID> {

  Optional<AppUserExternalIdentityJpaEntity> findByProviderAndIssuerAndExternalSubject(
      IdentityProviderType provider, String issuer, String externalSubject);

  Optional<AppUserExternalIdentityJpaEntity> findFirstByProviderAndExternalSubject(
      IdentityProviderType provider, String externalSubject);

  Optional<AppUserExternalIdentityJpaEntity> findFirstByAppUserIdAndProvider(
      UUID appUserId, IdentityProviderType provider);
}
