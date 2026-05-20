package com.tchalanet.server.platform.tenanttheme.internal.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for tenant_theme.
 * RLS policies MUST be enforced at DB level.
 */
@Repository
public interface TenantThemeJpaRepository extends JpaRepository<TenantThemeJpaEntity, UUID> {

  Optional<TenantThemeJpaEntity> findByTenantId(UUID tenantId);

  void deleteByTenantId(UUID tenantId);
}
