package com.tchalanet.server.core.subscription.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tenant_subscription (core/subscription).
 * RLS enforced via Postgres policies (tenant_id).
 */
@Repository
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

  /**
   * Find subscription by tenant_id (RLS enforced).
   * Maps to spec S7 (tenant-scoped persistence).
   */
  Optional<SubscriptionJpaEntity> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
}
