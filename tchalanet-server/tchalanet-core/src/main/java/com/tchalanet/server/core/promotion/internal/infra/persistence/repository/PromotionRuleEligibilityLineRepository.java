package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleEligibilityLineJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRuleEligibilityLineRepository extends JpaRepository<PromotionRuleEligibilityLineJpaEntity, UUID> {
    List<PromotionRuleEligibilityLineJpaEntity> findByRuleIdOrderByGameCodeAsc(UUID ruleId);

    List<PromotionRuleEligibilityLineJpaEntity> findByRuleIdIn(Collection<UUID> ruleIds);

    void deleteByRuleId(UUID ruleId);
}
