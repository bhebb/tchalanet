package com.tchalanet.server.core.tenant.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantUserJpaRepository extends JpaRepository<TenantUserJpaEntity, UUID> {
  List<TenantUserJpaEntity> findByTenantId(UUID tenantId);

  List<TenantUserJpaEntity> findByUserId(UUID userId);

  Optional<TenantUserJpaEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

  void deleteByTenantIdAndUserId(UUID tenantId, UUID userId);
}
