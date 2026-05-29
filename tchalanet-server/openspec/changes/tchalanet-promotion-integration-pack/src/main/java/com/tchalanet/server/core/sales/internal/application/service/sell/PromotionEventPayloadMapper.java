package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionEffect;
import com.tchalanet.server.core.sales.api.event.payload.PromotionEffectPayload;
import com.tchalanet.server.core.sales.api.event.payload.PromotionPayload;

final class PromotionEventPayloadMapper {
    private PromotionEventPayloadMapper() {}

    static PromotionPayload toPayload(PromotionDecision decision) {
        if (decision == null) return null;
        return new PromotionPayload(
            decision.decisionId() == null ? null : decision.decisionId().value().toString(),
            decision.status() == null ? null : decision.status().name(),
            decision.phase() == null ? null : decision.phase().name(),
            decision.evaluatedAt(),
            decision.contextHash(),
            decision.engineVersion(),
            decision.effects().stream().map(PromotionEventPayloadMapper::toPayload).toList(),
            decision.notices()
        );
    }

    private static PromotionEffectPayload toPayload(PromotionEffect e) {
        return new PromotionEffectPayload(
            e.ruleId() == null ? null : e.ruleId().value().toString(),
            e.type() == null ? null : e.type().name(),
            e.gameCode(),
            e.quantity(),
            e.amount(),
            e.currency(),
            e.appliesTo(),
            e.reason(),
            e.choiceMode() == null ? null : e.choiceMode().name()
        );
    }
}
