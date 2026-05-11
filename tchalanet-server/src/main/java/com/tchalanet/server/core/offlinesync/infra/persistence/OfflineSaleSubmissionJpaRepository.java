package com.tchalanet.server.core.offlinesync.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfflineSaleSubmissionJpaRepository extends JpaRepository<OfflineSaleSubmissionJpaEntity, UUID> {
  List<OfflineSaleSubmissionJpaEntity> findByBatchIdAndStatus(UUID batchId, String status);
  Optional<OfflineSaleSubmissionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
