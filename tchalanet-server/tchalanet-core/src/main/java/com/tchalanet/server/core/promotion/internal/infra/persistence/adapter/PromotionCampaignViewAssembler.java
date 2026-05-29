package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.infra.persistence.mapper.PromotionCampaignJpaMapper;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionCampaignRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleEffectRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleEligibilityLineRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PromotionCampaignViewAssembler {

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRuleRepository ruleRepository;
    private final PromotionRuleEffectRepository effectRepository;
    private final PromotionRuleEligibilityLineRepository eligibilityLineRepository;
    private final PromotionCampaignJpaMapper mapper;

    PromotionCampaignView toCampaignView(UUID campaignId) {
        var campaign = campaignRepository.getRequired(campaignId);
        var rules = ruleRepository.findByCampaignIdOrderByPriorityAscRuleKeyAsc(campaignId);
        var views = rules.stream()
            .map(rule -> mapper.toRuleView(
                rule,
                eligibilityLineRepository.findByRuleIdOrderByGameCodeAsc(rule.getId()),
                effectRepository.findByRuleIdOrderByIdAsc(rule.getId())))
            .toList();
        return mapper.toView(campaign, views);
    }
}
