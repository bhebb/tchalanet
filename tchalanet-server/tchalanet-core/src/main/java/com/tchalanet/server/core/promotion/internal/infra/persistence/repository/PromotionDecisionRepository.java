package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionDecisionJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionDecisionRepository
    extends JpaRepository<PromotionDecisionJpaEntity, UUID> {
  // No tenant_id filter. RLS handles tenant isolation.
  Optional<PromotionDecisionJpaEntity> findByContextHashAndEvaluationPhase(
      String contextHash, String evaluationPhase);
}
