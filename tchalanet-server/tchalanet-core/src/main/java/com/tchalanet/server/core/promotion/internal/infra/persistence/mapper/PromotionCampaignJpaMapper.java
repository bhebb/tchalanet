package com.tchalanet.server.core.promotion.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.command.lifecycle.CreatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.PromotionEffectConfigView;
import com.tchalanet.server.core.promotion.api.model.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.PromotionEligibilityConfigView;
import com.tchalanet.server.core.promotion.api.model.PromotionEligibilityType;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.model.PromotionRuleStatus;
import com.tchalanet.server.core.promotion.api.model.PromotionRuleView;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionCampaignJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleJpaEntity;
import org.mapstruct.Mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface PromotionCampaignJpaMapper {
    default void updateFromCreate(CreatePromotionCampaignCommand cmd, PromotionCampaignJpaEntity e) {
        e.setCode(cmd.name()); //todo Nulll
        e.setName(cmd.name());
        e.setStatus(PromotionCampaignStatus.DRAFT);
        e.setPriority(cmd.priority());
        e.setStartsAt(cmd.startsAt());
        e.setEndsAt(cmd.endsAt());
    }

    default void updateFromUpdate(com.tchalanet.server.core.promotion.api.command.lifecycle.UpdatePromotionCampaignCommand cmd, PromotionCampaignJpaEntity e) {
        e.setName(cmd.name());
        e.setPriority(cmd.priority());
        e.setStartsAt(cmd.startsAt());
        e.setPriority(cmd.priority());
        e.setStartsAt(cmd.startsAt());
        e.setEndsAt(cmd.endsAt());
    }


    default PromotionCampaignView toView(
        PromotionCampaignJpaEntity campaign,
        List<PromotionRuleJpaEntity> rules
    ) {
        return new PromotionCampaignView(
            PromotionCampaignId.of(campaign.getId()),
            campaign.getCode(),
            campaign.getName(),
            campaign.getStatus(),
            campaign.getPriority(),
            campaign.getStartsAt(),
            campaign.getEndsAt(),
            rules == null ? List.of() : rules.stream().map(this::toRuleView).toList()
        );
    }

    default PromotionRuleView toRuleView(PromotionRuleJpaEntity rule) {
        return new PromotionRuleView(
            PromotionRuleId.of(rule.getId()),
            rule.getRuleKey(),
            parseRuleStatus(rule.getStatus()),
            parseEvaluationPhase(rule.getEvaluationPhase()),
            rule.getPriority(),
            toEligibilityViews(rule.getEligibilityJson()),
            toEffectViews(rule.getEffectsJson()),
            rule.getQuotaKey(),
            rule.getMaxUses()
        );
    }

    default List<PromotionEligibilityConfigView> toEligibilityViews(Map<String, Object> json) {
        return readItems(json).stream()
            .map(this::toEligibilityView)
            .toList();
    }

    default List<PromotionEffectConfigView> toEffectViews(Map<String, Object> json) {
        return readItems(json).stream()
            .map(this::toEffectView)
            .toList();
    }

    default PromotionEligibilityConfigView toEligibilityView(Map<String, Object> item) {
        var copy = new LinkedHashMap<>(item);
        var typeRaw = copy.remove("type");

        if (typeRaw == null) {
            typeRaw = copy.remove("conditionType");
        }

        if (typeRaw == null) {
            typeRaw = copy.remove("eligibilityType");
        }

        var type = PromotionEligibilityType.valueOf(String.valueOf(typeRaw));

        return new PromotionEligibilityConfigView(type, copy);
    }

    default PromotionEffectConfigView toEffectView(Map<String, Object> item) {
        var copy = new LinkedHashMap<>(item);
        var typeRaw = copy.remove("type");

        if (typeRaw == null) {
            typeRaw = copy.remove("effectType");
        }

        var type = PromotionEffectType.valueOf(String.valueOf(typeRaw));

        return new PromotionEffectConfigView(type, copy);
    }

    @SuppressWarnings("unchecked")
    default List<Map<String, Object>> readItems(Map<String, Object> json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }

        var raw = json.get("items");

        if (raw == null) {
            return List.of();
        }

        if (!(raw instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
            .filter(Map.class::isInstance)
            .map(item -> (Map<String, Object>) item)
            .toList();
    }

    default PromotionRuleStatus parseRuleStatus(String value) {
        return value == null ? null : PromotionRuleStatus.valueOf(value);
    }

    default PromotionEvaluationPhase parseEvaluationPhase(String value) {
        return value == null ? null : PromotionEvaluationPhase.valueOf(value);
    }
}
