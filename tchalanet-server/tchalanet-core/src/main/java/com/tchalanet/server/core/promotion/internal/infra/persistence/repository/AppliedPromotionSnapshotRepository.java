package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.AppliedPromotionSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AppliedPromotionSnapshotRepository extends JpaRepository<AppliedPromotionSnapshotJpaEntity, UUID> {
    // No tenant_id filter. RLS handles tenant isolation.
    Optional<AppliedPromotionSnapshotJpaEntity> findByTicketIdAndPromotionDecisionId(UUID ticketId, UUID promotionDecisionId);
}
