package com.tchalanet.server.core.promotion.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.command.lifecycle.CreatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.lifecycle.UpdatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityConfigView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEligibilityType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionRuleView;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleEffectJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleEligibilityLineJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleJpaEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromotionCampaignJpaMapper {
    default void updateFromCreate(CreatePromotionCampaignCommand cmd, PromotionCampaignJpaEntity e) {
        e.setCode(cmd.name());
        e.setName(cmd.name());
        e.setStatus(PromotionCampaignStatus.DRAFT);
        e.setPriority(cmd.priority());
        e.setStartsAt(cmd.startsAt());
        e.setEndsAt(cmd.endsAt());
    }

    default void updateFromUpdate(UpdatePromotionCampaignCommand cmd, PromotionCampaignJpaEntity e) {
        e.setName(cmd.name());
        e.setPriority(cmd.priority());
        e.setStartsAt(cmd.startsAt());
        e.setEndsAt(cmd.endsAt());
    }

    default PromotionCampaignView toView(
        PromotionCampaignJpaEntity campaign,
        List<PromotionRuleView> rules
    ) {
        return new PromotionCampaignView(
            PromotionCampaignId.of(campaign.getId()),
            campaign.getCode(),
            campaign.getName(),
            campaign.getStatus(),
            campaign.getPriority(),
            campaign.getStartsAt(),
            campaign.getEndsAt(),
            rules == null ? List.of() : List.copyOf(rules)
        );
    }

    default PromotionRuleView toRuleView(
        PromotionRuleJpaEntity rule,
        List<PromotionRuleEligibilityLineJpaEntity> eligibilityLines,
        List<PromotionRuleEffectJpaEntity> effects
    ) {
        var eligibility = new java.util.ArrayList<PromotionEligibilityConfigView>();
        if (rule.getMinPaidTotal() != null) {
            eligibility.add(new PromotionEligibilityConfigView(
                PromotionEligibilityType.MIN_PAID_TOTAL,
                Map.of("amount", rule.getMinPaidTotal())
            ));
        }
        if (rule.getBeforeLocalTime() != null) {
            eligibility.add(new PromotionEligibilityConfigView(
                PromotionEligibilityType.BEFORE_LOCAL_TIME,
                Map.of("time", rule.getBeforeLocalTime().toString())
            ));
        }
        if (eligibilityLines != null) {
            eligibility.addAll(eligibilityLines.stream()
                .map(line -> new PromotionEligibilityConfigView(
                    PromotionEligibilityType.PAID_LINE_COUNT,
                    Map.of("gameCode", line.getGameCode(), "minCount", line.getMinCount())))
                .toList());
        }

        return new PromotionRuleView(
            PromotionRuleId.of(rule.getId()),
            rule.getRuleKey(),
            rule.getPriority(),
            eligibility,
            effects == null ? List.of() : effects.stream().map(this::toEffectView).toList()
        );
    }

    default PromotionEffectConfigView toEffectView(PromotionRuleEffectJpaEntity effect) {
        var params = new LinkedHashMap<String, Object>();
        if (effect.getGameCode() != null) {
            params.put("gameCode", effect.getGameCode());
        }
        if (effect.getPayoutBaseAmount() != null) {
            params.put("payoutBaseAmount", effect.getPayoutBaseAmount());
        }
        if (effect.getQuantity() != null) {
            params.put("quantity", effect.getQuantity());
        }
        if (effect.getOddsOverride() != null) {
            params.put("oddsOverride", effect.getOddsOverride());
        }
        if (effect.getChargeType() != null) {
            params.put("chargeType", effect.getChargeType());
        }
        if (effect.getChoiceMode() != null) {
            params.put("choiceMode", effect.getChoiceMode().name());
        }
        if (effect.getGenerationStrategy() != null) {
            params.put("generationStrategy", effect.getGenerationStrategy().name());
            params.put("regenerableBeforeConfirm", effect.isRegenerableBeforeConfirm());
            params.put("maxRegenerationsBeforeConfirm", effect.getMaxRegenerationsBeforeConfirm());
        }
        return new PromotionEffectConfigView(effect.getEffectType(), params);
    }
}
