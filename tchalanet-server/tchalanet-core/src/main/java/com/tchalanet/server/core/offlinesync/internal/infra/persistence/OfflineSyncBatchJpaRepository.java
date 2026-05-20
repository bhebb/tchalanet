package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfflineSyncBatchJpaRepository extends JpaRepository<OfflineSyncBatchJpaEntity, UUID> {

    Optional<OfflineSyncBatchJpaEntity> findByTenantIdAndGrantIdAndClientBatchId(
        UUID tenantId, UUID grantId, String clientBatchId);
}
