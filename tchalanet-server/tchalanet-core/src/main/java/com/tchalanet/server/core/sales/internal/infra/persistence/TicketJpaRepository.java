package com.tchalanet.server.core.sales.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketJpaRepository extends JpaRepository<TicketJpaEntity, UUID> {
    Optional<TicketJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByTenantIdAndOfflineCodeAndSaleOriginAndSyncStatus(UUID tenantId, String offlineCode, String saleOrigin, String syncStatus);

}
