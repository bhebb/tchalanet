package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.infra.persistence.mapper.PromotionCampaignJpaMapper;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionCampaignRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PromotionCampaignViewAssembler {

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRuleRepository ruleRepository;
    private final PromotionCampaignJpaMapper mapper;

    PromotionCampaignView toCampaignView(UUID campaignId) {
        var campaign = campaignRepository.getRequired(campaignId);
        var rules = ruleRepository.findByCampaignIdOrderByPriorityAscRuleKeyAsc(campaignId);
        return mapper.toView(campaign, rules);
    }
}
