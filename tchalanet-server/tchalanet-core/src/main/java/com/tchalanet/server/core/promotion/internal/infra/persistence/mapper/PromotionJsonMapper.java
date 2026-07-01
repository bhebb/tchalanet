package com.tchalanet.server.core.promotion.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionDecisionStatus;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityTier;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.UUID;

public class PromotionJsonMapper {
    private PromotionJsonMapper() {}

    /** Reconstruct a {@link PromotionDecision} from the {@code decision_json} column. */
    @SuppressWarnings("unchecked")
    public static PromotionDecision fromJson(UUID id, Map<String, Object> json) {
        var effectsRaw = json.get("effects");
        var effects = (effectsRaw instanceof List<?> list)
            ? list.stream()
                .filter(e -> e instanceof Map<?, ?>)
                .map(e -> effectFromJson((Map<String, Object>) e))
                .toList()
            : List.<PromotionEffect>of();

        var noticesRaw = json.get("notices");
        var notices = (noticesRaw instanceof List<?> list)
            ? list.stream().filter(n -> n instanceof String).map(n -> (String) n).toList()
            : List.<String>of();

        return new PromotionDecision(
            PromotionDecisionId.of(id),
            PromotionDecisionStatus.valueOf((String) json.get("status")),
            PromotionEvaluationPhase.valueOf((String) json.get("phase")),
            Instant.parse((String) json.get("evaluatedAt")),
            (String) json.get("contextHash"),
            (String) json.get("engineVersion"),
            effects,
            notices
        );
    }

    @SuppressWarnings("unchecked")
    private static PromotionEffect effectFromJson(Map<String, Object> m) {
        var ruleIdRaw = m.get("ruleId");
        var ruleId = ruleIdRaw != null ? PromotionRuleId.of(UUID.fromString((String) ruleIdRaw)) : null;
        var campaignIdRaw = m.get("campaignId");
        var campaignId = campaignIdRaw != null ? PromotionCampaignId.of(UUID.fromString((String) campaignIdRaw)) : null;
        var ruleKey = (String) m.get("ruleKey");
        var typeRaw = m.get("type");
        var type = typeRaw != null ? PromotionEffectType.valueOf((String) typeRaw) : null;
        var choiceModeRaw = m.get("choiceMode");
        var choiceMode = choiceModeRaw != null ? PromotionChoiceMode.valueOf((String) choiceModeRaw) : null;
        var strategyRaw = m.get("generationStrategy");
        var strategy = strategyRaw != null ? SelectionGenerationStrategy.valueOf((String) strategyRaw) : null;
        var regenerable = Boolean.TRUE.equals(m.get("regenerableBeforeConfirm"));
        var maxRegenRaw = m.get("maxRegenerationsBeforeConfirm");
        int maxRegen = maxRegenRaw instanceof Number n
            ? n.intValue()
            : PromotionEffect.DEFAULT_MAX_REGENERATIONS;
        var amountRaw = m.get("amount");
        var amount = amountRaw instanceof Number n ? new BigDecimal(n.toString())
            : amountRaw != null ? new BigDecimal((String) amountRaw) : null;
        var quantityRaw = m.get("quantity");
        int quantity = quantityRaw instanceof Number n ? n.intValue() : 0;
        var quantityModeRaw = m.get("quantityMode");
        var quantityMode = quantityModeRaw != null
            ? PromotionQuantityMode.valueOf((String) quantityModeRaw)
            : PromotionQuantityMode.FIXED;
        var stepPaidAmountRaw = m.get("stepPaidAmount");
        var stepPaidAmount = stepPaidAmountRaw instanceof Number n ? new BigDecimal(n.toString())
            : stepPaidAmountRaw != null ? new BigDecimal((String) stepPaidAmountRaw) : null;
        var quantityPerStepRaw = m.get("quantityPerStep");
        int quantityPerStep = quantityPerStepRaw instanceof Number n
            ? n.intValue()
            : PromotionEffect.DEFAULT_QUANTITY_PER_STEP;
        var maxQuantityRaw = m.get("maxQuantity");
        int maxQuantity = maxQuantityRaw instanceof Number n ? n.intValue() : quantity;
        var quantityTiers = quantityTiersFromJson(m.get("quantityTiers"));
        return new PromotionEffect(
            ruleId,
            campaignId,
            ruleKey,
            type,
            (String) m.get("gameCode"),
            quantity,
            quantityMode,
            stepPaidAmount,
            quantityPerStep,
            maxQuantity,
            quantityTiers,
            amount,
            (String) m.get("currency"),
            (String) m.get("appliesTo"),
            (String) m.get("reason"),
            choiceMode,
            strategy,
            regenerable,
            maxRegen
        );
    }

    public static Map<String,Object> toJson(PromotionDecision decision) {
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("decisionId", decision.decisionId().value().toString());
        out.put("status", decision.status().name());
        out.put("phase", decision.phase().name());
        out.put("evaluatedAt", decision.evaluatedAt().toString());
        out.put("contextHash", decision.contextHash());
        out.put("engineVersion", decision.engineVersion());
        out.put("notices", decision.notices());
        out.put("effects", decision.effects().stream().map(PromotionJsonMapper::effectToJson).toList());
        return out;
    }

    public static Map<String,Object> effectToJson(PromotionEffect effect) {
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("ruleId", effect.ruleId() == null ? null : effect.ruleId().value().toString());
        out.put("campaignId", effect.campaignId() == null ? null : effect.campaignId().value().toString());
        out.put("ruleKey", effect.ruleKey());
        out.put("type", effect.type() == null ? null : effect.type().name());
        out.put("gameCode", effect.gameCode());
        out.put("quantity", effect.quantity());
        out.put("quantityMode", effect.quantityMode().name());
        out.put("stepPaidAmount", effect.stepPaidAmount());
        out.put("quantityPerStep", effect.quantityPerStep());
        out.put("maxQuantity", effect.maxQuantity());
        out.put("quantityTiers", effect.quantityTiers().stream().map(PromotionJsonMapper::quantityTierToJson).toList());
        out.put("amount", effect.amount());
        out.put("currency", effect.currency());
        out.put("appliesTo", effect.appliesTo());
        out.put("reason", effect.reason());
        out.put("choiceMode", effect.choiceMode() == null ? null : effect.choiceMode().name());
        out.put("generationStrategy", effect.generationStrategy() == null ? null : effect.generationStrategy().name());
        out.put("regenerableBeforeConfirm", effect.regenerableBeforeConfirm());
        out.put("maxRegenerationsBeforeConfirm", effect.maxRegenerationsBeforeConfirm());
        return out;
    }

    private static List<PromotionQuantityTier> quantityTiersFromJson(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
            .filter(item -> item instanceof Map<?, ?>)
            .map(item -> {
                var map = (Map<?, ?>) item;
                return new PromotionQuantityTier(
                    decimal(map.get("minPaidAmount")),
                    map.get("maxPaidAmount") == null ? null : decimal(map.get("maxPaidAmount")),
                    integer(map.get("quantity"))
                );
            })
            .toList();
    }

    private static Map<String, Object> quantityTierToJson(PromotionQuantityTier tier) {
        var out = new LinkedHashMap<String, Object>();
        out.put("minPaidAmount", tier.minPaidAmount());
        out.put("maxPaidAmount", tier.maxPaidAmount());
        out.put("quantity", tier.quantity());
        return out;
    }

    private static BigDecimal decimal(Object value) {
        return value instanceof Number number
            ? new BigDecimal(number.toString())
            : new BigDecimal(String.valueOf(value));
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }
}
