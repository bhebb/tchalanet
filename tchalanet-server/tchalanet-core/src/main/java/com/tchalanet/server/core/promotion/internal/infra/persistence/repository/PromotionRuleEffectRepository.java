package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleEffectJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRuleEffectRepository extends JpaRepository<PromotionRuleEffectJpaEntity, UUID> {
    List<PromotionRuleEffectJpaEntity> findByRuleIdOrderByIdAsc(UUID ruleId);

    List<PromotionRuleEffectJpaEntity> findByRuleIdIn(Collection<UUID> ruleIds);

    void deleteByRuleId(UUID ruleId);
}
