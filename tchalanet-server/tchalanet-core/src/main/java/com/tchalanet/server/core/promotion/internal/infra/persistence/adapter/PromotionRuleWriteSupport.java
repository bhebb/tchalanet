package com.tchalanet.server.core.promotion.internal.infra.persistence.adapter;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.promotion.api.command.rule.AddPromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.DeletePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEligibilityCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.PromotionEffectConfigInput;
import com.tchalanet.server.core.promotion.api.model.PromotionEligibilityConfigInput;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleJpaEntity;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionCampaignRepository;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

@Component
@RequiredArgsConstructor
class PromotionRuleWriteSupport {

    private static final String RULE_STATUS_ACTIVE = "ACTIVE";
    private static final String RULE_STATUS_INACTIVE = "INACTIVE";
    private static final String JSON_ITEMS = "items";

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
        new TypeReference<>() {};

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRuleRepository ruleRepository;
    private final PromotionCampaignViewAssembler viewAssembler;
    private final JsonUtils objectMapper;

    PromotionCampaignView addRule(AddPromotionRuleCommand cmd) {
        var campaign = campaignRepository.getRequired(cmd.campaignId().value());

        ensureRuleKeyAvailable(campaign.getId(), cmd.ruleKey());

        var rule = new PromotionRuleJpaEntity();
        rule.setCampaignId(campaign.getId());
        rule.setRuleKey(requiredText(cmd.ruleKey(), "promotion.rule.rule_key_required"));
        rule.setStatus(toRuleStatus(cmd.active()));
        rule.setEvaluationPhase(requiredPhase(cmd.phase()));
        rule.setPriority(cmd.priority() == null ? 100 : cmd.priority());

        rule.setEligibilityJson(toEligibilityJson(cmd.eligibilityItems()));
        rule.setEffectsJson(toEffectsJson(cmd.effectItems()));

        rule.setQuotaKey(blankToNull(cmd.quotaKey()));
        rule.setMaxUses(cmd.maxUses());

        validateQuotaFields(rule);

        ruleRepository.save(rule);

        return viewAssembler.toCampaignView(campaign.getId());
    }

    PromotionCampaignView updateRule(UpdatePromotionRuleCommand cmd) {
        var rule = getRuleRequired(cmd.campaignId().value(), cmd.ruleId().value());

        if (cmd.ruleKey() != null && !cmd.ruleKey().isBlank()) {
            if (!cmd.ruleKey().equals(rule.getRuleKey())) {
                ensureRuleKeyAvailable(cmd.campaignId().value(), cmd.ruleKey());
            }
            rule.setRuleKey(cmd.ruleKey());
        }

        if (cmd.phase() != null) {
            rule.setEvaluationPhase(cmd.phase().name());
        }

        if (cmd.priority() != null) {
            rule.setPriority(cmd.priority());
        }

        if (cmd.active() != null) {
            rule.setStatus(cmd.active() ? RULE_STATUS_ACTIVE : RULE_STATUS_INACTIVE);
        }

        /*
         * Si tu ajoutes quotaKey / maxUses dans UpdatePromotionRuleCommand,
         * applique-les ici.
         *
         * Exemple :
         * if (cmd.quotaKey() != null) rule.setQuotaKey(blankToNull(cmd.quotaKey()));
         * if (cmd.maxUses() != null) rule.setMaxUses(cmd.maxUses());
         */

        validateQuotaFields(rule);

        ruleRepository.save(rule);

        return viewAssembler.toCampaignView(cmd.campaignId().value());
    }

    PromotionCampaignView updateRuleEligibility(UpdatePromotionRuleEligibilityCommand cmd) {
        var rule = getRuleRequired(cmd.campaignId().value(), cmd.ruleId().value());

        rule.setEligibilityJson(toEligibilityJson(cmd.items()));

        ruleRepository.save(rule);

        return viewAssembler.toCampaignView(cmd.campaignId().value());
    }

    PromotionCampaignView updateRuleEffects(UpdatePromotionRuleEffectsCommand cmd) {
        var rule = getRuleRequired(cmd.campaignId().value(), cmd.ruleId().value());

        rule.setEffectsJson(toEffectsJson(cmd.items()));

        ruleRepository.save(rule);

        return viewAssembler.toCampaignView(cmd.campaignId().value());
    }

    PromotionCampaignView deleteRule(DeletePromotionRuleCommand cmd) {
        var rule = getRuleRequired(cmd.campaignId().value(), cmd.ruleId().value());

        /*
         * Si BaseTenantEntity a soft-delete, préfère markDeleted().
         * Sinon delete physique pour V1.
         */
        ruleRepository.delete(rule);

        return viewAssembler.toCampaignView(cmd.campaignId().value());
    }

    private PromotionRuleJpaEntity getRuleRequired(UUID campaignId, UUID ruleId) {
        return ruleRepository.findByIdAndCampaignId(ruleId, campaignId)
            .orElseThrow(() -> ProblemRest.notFound("promotion.rule.not_found"));
    }

    private void ensureRuleKeyAvailable(UUID campaignId, String ruleKey) {
        if (ruleKey == null || ruleKey.isBlank()) {
            throw ProblemRest.badRequest("promotion.rule.rule_key_required");
        }

        if (ruleRepository.existsByCampaignIdAndRuleKey(campaignId, ruleKey)) {
            throw ProblemRest.conflict("promotion.rule.rule_key_already_exists");
        }
    }

    private Map<String, Object> toEligibilityJson(List<PromotionEligibilityConfigInput> items) {
        if (items == null) {
            return jsonItems(List.of());
        }

        var jsonItems = items.stream()
            .map(this::toJsonMap)
            .peek(this::validateEligibilityItem)
            .toList();

        return jsonItems(jsonItems);
    }

    private Map<String, Object> toEffectsJson(List<PromotionEffectConfigInput> items) {
        if (items == null || items.isEmpty()) {
            throw ProblemRest.badRequest("promotion.rule.effects_required");
        }

        var jsonItems = items.stream()
            .map(this::toJsonMap)
            .peek(this::validateEffectItem)
            .toList();

        return jsonItems(jsonItems);
    }

    private Map<String, Object> jsonItems(List<?> items) {
        var json = new LinkedHashMap<String, Object>();
        json.put(JSON_ITEMS, items == null ? List.of() : items);
        return json;
    }

    private Map<String, Object> toJsonMap(Object value) {
        return objectMapper.convertValue(value, MAP_TYPE);
    }

    private void validateEligibilityItem(Map<String, Object> item) {
        var type = textValue(item, "type", "conditionType", "eligibilityType");

        if (type == null || type.isBlank()) {
            throw ProblemRest.badRequest("promotion.rule.eligibility_type_required");
        }

        /*
         * V1 conditions autorisées :
         * - MIN_PAID_TOTAL
         * - PAID_LINE_COUNT
         * - BEFORE_LOCAL_TIME
         * - STARTS_AT / ENDS_AT si tu les mets au niveau rule
         *
         * Si startsAt/endsAt restent sur campaign, pas besoin ici.
         */
        switch (type) {
            case "MIN_PAID_TOTAL" -> requireAny(item, "amount", "minPaidTotal");
            case "PAID_LINE_COUNT" -> {
                requireAny(item, "gameCode");
                requireAny(item, "minCount", "count");
            }
            case "BEFORE_LOCAL_TIME" -> requireAny(item, "time", "beforeLocalTime");
            default -> throw ProblemRest.badRequest("promotion.rule.eligibility_type_unsupported");
        }
    }

    private void validateEffectItem(Map<String, Object> item) {
        var type = textValue(item, "type", "effectType");

        if (type == null || type.isBlank()) {
            throw ProblemRest.badRequest("promotion.rule.effect_type_required");
        }

        /*
         * V1 effects autorisés :
         * - FREE_GAME_LINE
         * - BOOST_ODDS
         * - WAIVE_CHARGE
         */
        switch (type) {
            case "FREE_GAME_LINE" -> {
                requireAny(item, "gameCode");
                requireAny(item, "payoutBaseAmount");
            }
            case "BOOST_ODDS" -> {
                requireAny(item, "gameCode");
                requireAny(item, "oddsOverride");
            }
            case "WAIVE_CHARGE" -> requireAny(item, "chargeCode");
            default -> throw ProblemRest.badRequest("promotion.rule.effect_type_unsupported");
        }
    }

    private String toRuleStatus(Boolean active) {
        return Boolean.FALSE.equals(active) ? RULE_STATUS_INACTIVE : RULE_STATUS_ACTIVE;
    }

    private String requiredPhase(Object phase) {
        if (phase == null) {
            throw ProblemRest.badRequest("promotion.rule.phase_required");
        }
        return String.valueOf(phase);
    }

    private String requiredText(String value, String errorCode) {
        if (value == null || value.isBlank()) {
            throw ProblemRest.badRequest(errorCode);
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void validateQuotaFields(PromotionRuleJpaEntity rule) {
        if (rule.getMaxUses() != null && rule.getMaxUses() <= 0) {
            throw ProblemRest.badRequest("promotion.rule.max_uses_must_be_positive");
        }

        if (rule.getMaxUses() != null && (rule.getQuotaKey() == null || rule.getQuotaKey().isBlank())) {
            throw ProblemRest.badRequest("promotion.rule.quota_key_required_when_max_uses_set");
        }
    }

    private void requireAny(Map<String, Object> item, String... keys) {
        for (var key : keys) {
            var value = item.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return;
            }
        }

        throw ProblemRest.badRequest("promotion.rule.required_field_missing");
    }

    private String textValue(Map<String, Object> item, String... keys) {
        for (var key : keys) {
            var value = item.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
