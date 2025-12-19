package com.tchalanet.server.core.user.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAppUserRepository extends JpaRepository<AppUserJpaEntity, UUID> {
  Optional<AppUserJpaEntity> findByKeycloakId(UUID keycloakId);
  Optional<AppUserJpaEntity> findByEmail(String email);
  Optional<AppUserJpaEntity> findByPhone(String phone);
  Optional<AppUserJpaEntity> findByEmailOrPhone(String email, String phone);
  List<AppUserJpaEntity> findByStatusAndDeletedAtIsNull(String status);
  List<AppUserJpaEntity> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, String status);
  Optional<AppUserJpaEntity> findByKeycloakIdAndDeletedAtIsNull(UUID keycloakId);

  // Paged
  Page<AppUserJpaEntity> findAll(Pageable pageable);
  Page<AppUserJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);
  Page<AppUserJpaEntity> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);
  Page<AppUserJpaEntity> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, String status, Pageable pageable);
}
