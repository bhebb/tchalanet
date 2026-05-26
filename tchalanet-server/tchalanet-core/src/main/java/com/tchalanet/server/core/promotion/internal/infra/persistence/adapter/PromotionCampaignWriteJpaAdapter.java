package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.command.rule.AddPromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.lifecycle.CreatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.rule.DeletePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.lifecycle.UpdatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEligibilityCommand;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle.PromotionCampaignWritePort;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.mapper.PromotionCampaignJpaMapper;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionCampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PromotionCampaignWriteJpaAdapter implements PromotionCampaignWritePort {

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionCampaignViewAssembler promotionCampaignViewAssembler;
    private final PromotionCampaignJpaMapper mapper;
    private final PromotionRuleWriteSupport ruleWriteSupport;

    @Override
    public PromotionCampaignView create(CreatePromotionCampaignCommand cmd) {
        var e = new PromotionCampaignJpaEntity();

        mapper.updateFromCreate(cmd, e);

        if (e.getStatus() == null) {
            e.setStatus(PromotionCampaignStatus.DRAFT);
        }

        var saved = campaignRepository.save(e);
        ruleWriteSupport.addRules(saved, cmd.rules());
        return promotionCampaignViewAssembler.toCampaignView(saved.getId());
    }

    @Override
    public PromotionCampaignView update(UpdatePromotionCampaignCommand cmd) {
        var e = campaignRepository.getRequired(cmd.campaignId().value());

        mapper.updateFromUpdate(cmd, e);

        // Entity is managed. save() is not strictly required after getRequired(),
        // but keeping it is acceptable and explicit.
        campaignRepository.save(e);

        return promotionCampaignViewAssembler.toCampaignView(cmd.campaignId().value());
    }


    @Override
    public PromotionCampaignView changeStatus(
        TenantId tenantId,
        PromotionCampaignId campaignId,
        PromotionCampaignStatus status
    ) {
        var e = campaignRepository.getRequired(campaignId.value());
        e.setStatus(status);
        campaignRepository.save(e);
        return promotionCampaignViewAssembler.toCampaignView(campaignId.value());
    }

    @Override
    public PromotionCampaignView addRule(AddPromotionRuleCommand cmd) {
        return ruleWriteSupport.addRule(cmd);
    }

    @Override
    public PromotionCampaignView updateRule(UpdatePromotionRuleCommand cmd) {
        return ruleWriteSupport.updateRule(cmd);
    }

    @Override
    public PromotionCampaignView deleteRule(DeletePromotionRuleCommand cmd) {
        return ruleWriteSupport.deleteRule(cmd);
    }

    @Override
    public PromotionCampaignView updateRuleEffects(UpdatePromotionRuleEffectsCommand cmd) {
        return ruleWriteSupport.updateRuleEffects(cmd);
    }

    @Override
    public PromotionCampaignView updateRuleEligibility(UpdatePromotionRuleEligibilityCommand cmd) {
        return ruleWriteSupport.updateRuleEligibility(cmd);
    }
}
