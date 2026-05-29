package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.internal.application.port.out.AppliedPromotionSnapshotWriterPort;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.AppliedPromotionSnapshotJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.AppliedPromotionSnapshotRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AppliedPromotionSnapshotJpaAdapter implements AppliedPromotionSnapshotWriterPort {
    private final AppliedPromotionSnapshotRepository repository;

    @Override
    public void createIfAbsent(TicketId ticketId, PromotionDecision decision, Ticket ticket, Instant appliedAt) {
        if (decision == null || decision.decisionId() == null) {
            return;
        }
        if (repository.findByTicketIdAndPromotionDecisionId(ticketId.value(), decision.decisionId().value()).isPresent()) {
            return;
        }

        var entity = new AppliedPromotionSnapshotJpaEntity();
        entity.setTicketId(ticketId.value());
        entity.setPromotionDecisionId(decision.decisionId().value());
        entity.setDecisionStatus(decision.status().name());
        entity.setAppliedAt(appliedAt);
        entity.setSnapshotJson(toSnapshotJson(decision, ticket, appliedAt));
        repository.save(entity);
    }

    private Map<String, Object> toSnapshotJson(PromotionDecision decision, Ticket ticket, Instant appliedAt) {
        var json = new LinkedHashMap<String, Object>();
        json.put("decisionId", decision.decisionId().value().toString());
        json.put("status", decision.status().name());
        json.put("phase", decision.phase().name());
        json.put("evaluatedAt", decision.evaluatedAt().toString());
        json.put("appliedAt", appliedAt.toString());
        json.put("contextHash", decision.contextHash());
        json.put("engineVersion", decision.engineVersion());
        json.put("notices", decision.notices());
        json.put("effects", decision.effects().stream().map(this::toEffectJson).toList());
        json.put("materializedLineEffects", ticket.lines().stream()
            .filter(line -> decision.decisionId().equals(line.promotionDecisionId()))
            .map(this::toLineEffectJson)
            .toList());
        json.put("materializedChargeEffects", ticket.money().breakdown().charges().stream()
            .filter(charge -> charge.waivedByRuleId() != null)
            .map(this::toChargeEffectJson)
            .toList());
        return json;
    }

    private Map<String, Object> toEffectJson(PromotionEffect effect) {
        var json = new LinkedHashMap<String, Object>();
        json.put("ruleId", effect.ruleId() == null ? null : effect.ruleId().value().toString());
        json.put("campaignId", effect.campaignId() == null ? null : effect.campaignId().value().toString());
        json.put("campaignCode", null);
        json.put("ruleKey", effect.ruleKey());
        json.put("type", effect.type() == null ? null : effect.type().name());
        json.put("gameCode", effect.gameCode());
        json.put("quantity", effect.quantity());
        json.put("amount", effect.amount());
        json.put("currency", effect.currency());
        json.put("appliesTo", effect.appliesTo());
        json.put("reason", effect.reason());
        json.put("choiceMode", effect.choiceMode() == null ? null : effect.choiceMode().name());
        return json;
    }

    private Map<String, Object> toLineEffectJson(TicketLine line) {
        var json = new LinkedHashMap<String, Object>();
        json.put("lineId", line.id().value().toString());
        json.put("lineNumber", line.lineNumber());
        json.put("gameCode", line.gameCode().name());
        json.put("betType", line.betType().name());
        json.put("betOption", line.betOption());
        json.put("origin", line.origin().name());
        json.put("pricingSource", line.pricingSource().name());
        json.put("selectionSource", line.selectionSource().name());
        json.put("stakeAmount", line.stakeAmount().amount());
        json.put("originalStakeAmount", line.origin().name().equals("PROMOTION") ? null : line.stakeAmount().amount());
        json.put("finalStakeAmount", line.stakeAmount().amount());
        json.put("payoutBaseAmount", line.payoutBaseAmount().amount());
        json.put("originalOddsSnapshot", line.pricingSource().name().equals("PROMOTION") ? null : line.oddsSnapshot());
        json.put("finalOddsSnapshot", line.oddsSnapshot());
        json.put("oddsSnapshot", line.oddsSnapshot());
        json.put("potentialPayoutAmount", line.potentialPayoutAmount().amount());
        json.put("finalPotentialPayoutAmount", line.potentialPayoutAmount().amount());
        json.put("promotionLabel", line.promotionLabel());
        json.put("promotionEffectType", line.promotionEffectType());
        return json;
    }

    private Map<String, Object> toChargeEffectJson(TicketCharge charge) {
        var json = new LinkedHashMap<String, Object>();
        json.put("chargeType", charge.type().name());
        json.put("originalChargeAmount", charge.amount().amount());
        json.put("finalAmount", charge.amount().amount());
        json.put("finalBuyerChargeAmount", charge.isWaived() ? java.math.BigDecimal.ZERO : charge.amount().amount());
        json.put("paidBy", charge.paidBy().name());
        json.put("waivedByRuleId", charge.waivedByRuleId().value().toString());
        return json;
    }
}
