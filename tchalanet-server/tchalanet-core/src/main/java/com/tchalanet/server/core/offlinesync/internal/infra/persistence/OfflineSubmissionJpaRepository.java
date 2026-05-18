package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfflineSubmissionJpaRepository extends JpaRepository<OfflineSubmissionJpaEntity, UUID> {

    long countByTenantIdAndStatus(UUID tenantId, String status);

    Optional<OfflineSubmissionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
