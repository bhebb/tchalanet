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
          join PromotionCampaignJpaEntity c on c.id = r.campaignId
        where c.status = com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus.ACTIVE
        order by c.priority asc, r.priority asc, r.ruleKey asc
        """)
    List<PromotionRuleJpaEntity> findActiveRules();


    Optional<PromotionRuleJpaEntity> findByIdAndCampaignId(UUID id, UUID campaignId);

    List<PromotionRuleJpaEntity> findByCampaignIdOrderByPriorityAscRuleKeyAsc(UUID campaignId);

    boolean existsByCampaignIdAndRuleKey(UUID campaignId, String ruleKey);
}
