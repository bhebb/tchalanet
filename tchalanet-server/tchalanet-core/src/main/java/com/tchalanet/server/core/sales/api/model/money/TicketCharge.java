package com.tchalanet.server.core.sales.api.model.money;

import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.money.Money;

import java.util.Objects;

/**
 * A single charge applied to a ticket sale, on top of the stake.
 *
 * <p>Only charges where {@code paidBy == BUYER && waivedByRuleId == null} contribute to the
 * ticket's {@code total}. Waived charges keep their original amount for audit
 * and print display but are excluded from the buyer-facing total.
 */
public record TicketCharge(
    TicketChargeType type,
    Money amount,
    ChargePaidBy paidBy,
    PromotionDecisionId waivedByDecisionId,
    PromotionRuleId waivedByRuleId,
    String waivedEffectType,
    String waivedLabel
) {
    public TicketCharge {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(paidBy, "paidBy is required");
        if (amount.amount().signum() < 0) {
            throw new IllegalArgumentException("Charge amount must not be negative");
        }
        waivedEffectType = normalize(waivedEffectType);
        waivedLabel = normalize(waivedLabel);
    }

    public TicketCharge(TicketChargeType type, Money amount, ChargePaidBy paidBy) {
        this(type, amount, paidBy, null, null, null, null);
    }

    public TicketCharge(
        TicketChargeType type,
        Money amount,
        ChargePaidBy paidBy,
        PromotionRuleId waivedByRuleId
    ) {
        this(type, amount, paidBy, null, waivedByRuleId, null, null);
    }

    public boolean isWaived() {
        return waivedByRuleId != null;
    }

    public boolean isBuyerFacing() {
        return paidBy == ChargePaidBy.BUYER && waivedByRuleId == null;
    }

    public TicketCharge asWaived(PromotionRuleId ruleId) {
        Objects.requireNonNull(ruleId, "ruleId is required");
        return new TicketCharge(type, amount, paidBy, null, ruleId, null, null);
    }

    public TicketCharge asWaived(
        PromotionDecisionId decisionId,
        PromotionRuleId ruleId,
        String effectType,
        String label
    ) {
        Objects.requireNonNull(decisionId, "decisionId is required");
        Objects.requireNonNull(ruleId, "ruleId is required");
        return new TicketCharge(type, amount, paidBy, decisionId, ruleId, effectType, label);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
