package com.tchalanet.server.core.promotion.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.core.promotion.api.model.*;
import com.tchalanet.server.core.promotion.internal.domain.model.*;
import com.tchalanet.server.core.promotion.internal.infra.persistence.entity.PromotionRuleJpaEntity;

import java.util.*;

public class PromotionJsonMapper {
    private PromotionJsonMapper() {}

    public static PromotionRule toDomain(PromotionRuleJpaEntity e) {
        return new PromotionRule(
            PromotionRuleId.of(e.getId()),
            com.tchalanet.server.common.types.id.PromotionCampaignId.of(e.getCampaignId()),
            e.getRuleKey(),
            PromotionRuleStatus.valueOf(e.getStatus()),
            PromotionEvaluationPhase.valueOf(e.getEvaluationPhase()),
            e.getEligibilityJson() == null ? Map.of() : Map.copyOf(e.getEligibilityJson()),
            e.getEffectsJson() == null ? Map.of() : Map.copyOf(e.getEffectsJson()),
            e.getQuotaKey(),
            e.getMaxUses()
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
        out.put("type", effect.type() == null ? null : effect.type().name());
        out.put("gameCode", effect.gameCode());
        out.put("quantity", effect.quantity());
        out.put("amount", effect.amount());
        out.put("currency", effect.currency());
        out.put("appliesTo", effect.appliesTo());
        out.put("reason", effect.reason());
        out.put("choiceMode", effect.choiceMode() == null ? null : effect.choiceMode().name());
        return out;
    }
}
