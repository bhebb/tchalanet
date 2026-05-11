package com.tchalanet.server.core.sales.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketJpaRepository extends JpaRepository<TicketJpaEntity, UUID> {
  Optional<TicketJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
  boolean existsByTenantIdAndOfflineCodeAndSaleOriginAndSyncStatus(UUID tenantId, String offlineCode, String saleOrigin, String syncStatus);
}
