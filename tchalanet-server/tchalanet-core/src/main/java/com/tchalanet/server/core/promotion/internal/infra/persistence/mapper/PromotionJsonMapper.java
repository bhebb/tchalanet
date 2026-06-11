package com.tchalanet.server.core.promotion.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.PromotionDecisionStatus;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
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
        return new PromotionEffect(
            ruleId,
            campaignId,
            ruleKey,
            type,
            (String) m.get("gameCode"),
            quantity,
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
}
