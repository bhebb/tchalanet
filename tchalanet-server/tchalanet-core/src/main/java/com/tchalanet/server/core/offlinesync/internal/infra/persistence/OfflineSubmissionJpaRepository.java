package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfflineSubmissionJpaRepository extends JpaRepository<OfflineSubmissionJpaEntity, UUID> {

    long countByTenantIdAndStatus(UUID tenantId, String status);

    Optional<OfflineSubmissionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<OfflineSubmissionJpaEntity> findByTenantIdAndGrantIdAndClientSubmissionId(
        UUID tenantId, UUID grantId, String clientSubmissionId);

    List<OfflineSubmissionJpaEntity> findAllByTenantIdAndSellerUserIdOrderByReceivedAtDesc(
        UUID tenantId, UUID sellerUserId, Pageable pageable);

    /**
     * Submissions stuck in promotionDecision: any of the listed statuses with
     * {@code promotion_requested_at < threshold}. Used by the recovery scheduler.
     */
    List<OfflineSubmissionJpaEntity> findAllByStatusInAndPromotionRequestedAtLessThan(
        Collection<String> statuses, Instant threshold);
}
