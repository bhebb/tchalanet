package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.AppliedPromotionSnapshotJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.AppliedPromotionSnapshotProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AppliedPromotionSnapshotProjectionRepository extends JpaRepository<AppliedPromotionSnapshotJpaEntity, UUID> {

    @Query("""
        SELECT new com.tchalanet.server.core.promotion.internal.infra.persistence.entity.AppliedPromotionSnapshotProjection(
            a.ticketId, a.promotionDecisionId, a.decisionStatus, a.appliedAt, a.snapshotJson
        )
        FROM AppliedPromotionSnapshotJpaEntity a
        WHERE a.promotionDecisionId = :decisionId
    """)
    Optional<AppliedPromotionSnapshotProjection> findByPromotionDecisionId(UUID decisionId);
}

