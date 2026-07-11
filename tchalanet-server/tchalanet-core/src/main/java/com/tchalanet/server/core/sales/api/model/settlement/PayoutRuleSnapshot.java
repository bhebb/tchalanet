package com.tchalanet.server.core.sales.api.model.settlement;

import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import java.math.BigDecimal;
import java.util.Objects;

public record PayoutRuleSnapshot(
    PayoutRuleType type,
    BigDecimal multiplier,
    BigDecimal fixedAmount
) {

    public PayoutRuleSnapshot {
        Objects.requireNonNull(type, "settlement.payout_rule.type_required");
        if (type == PayoutRuleType.STAKE_MULTIPLIER) {
            Objects.requireNonNull(multiplier, "settlement.payout_rule.multiplier_required");
            if (multiplier.signum() <= 0) {
                throw new IllegalArgumentException("settlement.payout_rule.multiplier_must_be_positive");
            }
            fixedAmount = null;
        } else {
            Objects.requireNonNull(fixedAmount, "settlement.payout_rule.fixed_amount_required");
            if (fixedAmount.signum() < 0) {
                throw new IllegalArgumentException("settlement.payout_rule.fixed_amount_must_be_non_negative");
            }
            multiplier = null;
        }
    }

    public static PayoutRuleSnapshot stakeMultiplier(BigDecimal multiplier) {
        return new PayoutRuleSnapshot(PayoutRuleType.STAKE_MULTIPLIER, multiplier, null);
    }

    public static PayoutRuleSnapshot fixedAmount(BigDecimal fixedAmount) {
        return new PayoutRuleSnapshot(PayoutRuleType.FIXED_AMOUNT, null, fixedAmount);
    }
}
