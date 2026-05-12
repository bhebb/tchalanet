package com.tchalanet.server.platform.address.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for Address persistence.
 * Includes tenant-scoped queries for deduplication.
 */
public interface AddressJpaRepository extends JpaRepository<AddressJpaEntity, UUID> {

  /**
   * Find address by ID and tenant (active only).
   */
  Optional<AddressJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

  /**
   * Find address by tenant and normalized key (active only).
   * Used for dedup lookup.
   */
  Optional<AddressJpaEntity> findByTenantIdAndNormalizedKeyAndDeletedFalse(UUID tenantId, String normalizedKey);

  /**
   * Find all active addresses for a tenant.
   * MVP: used to check if exactly one address exists.
   */
  List<AddressJpaEntity> findByTenantIdAndDeletedFalse(UUID tenantId);
}
