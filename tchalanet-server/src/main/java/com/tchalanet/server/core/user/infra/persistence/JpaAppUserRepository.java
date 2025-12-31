package com.tchalanet.server.core.user.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAppUserRepository extends JpaRepository<AppUserJpaEntity, UUID> {
  Optional<AppUserJpaEntity> findByKeycloakId(UUID keycloakId);

  Optional<AppUserJpaEntity> findByEmail(String email);

  Optional<AppUserJpaEntity> findByPhone(String phone);

  Optional<AppUserJpaEntity> findByEmailOrPhone(String email, String phone);

  // Paged
  Page<AppUserJpaEntity> findAll(Pageable pageable);

  @RestResource(path = "by-status", rel = "by-status")
  Page<AppUserJpaEntity> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);
}
