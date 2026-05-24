package com.tchalanet.server.core.promotion.internal.infra.persistence.repository;

import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRuleRepository extends JpaRepository<PromotionRuleJpaEntity, UUID> {
    @Query("""
        select r
        from PromotionRuleJpaEntity r
        where r.status = 'ACTIVE'
          and r.evaluationPhase = :phase
        order by r.ruleKey asc
        """)
    List<PromotionRuleJpaEntity> findActiveRulesForPhase(String phase);


    Optional<PromotionRuleJpaEntity> findByIdAndCampaignId(UUID id, UUID campaignId);

    List<PromotionRuleJpaEntity> findByCampaignIdOrderByPriorityAscRuleKeyAsc(UUID campaignId);

    boolean existsByCampaignIdAndRuleKey(UUID campaignId, String ruleKey);
}

