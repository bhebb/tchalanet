package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionDecisionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PromotionDecisionRepository extends JpaRepository<PromotionDecisionJpaEntity, UUID> {
    // No tenant_id filter. RLS handles tenant isolation.
    Optional<PromotionDecisionJpaEntity> findByContextHashAndEvaluationPhase(String contextHash, String evaluationPhase);
}
