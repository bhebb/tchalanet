package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.core.sales.internal.infra.persistence.entity.AppliedPromotionSnapshotJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedPromotionSnapshotRepository extends JpaRepository<AppliedPromotionSnapshotJpaEntity, UUID> {
    Optional<AppliedPromotionSnapshotJpaEntity> findByTicketIdAndPromotionDecisionId(UUID ticketId, UUID promotionDecisionId);
}
