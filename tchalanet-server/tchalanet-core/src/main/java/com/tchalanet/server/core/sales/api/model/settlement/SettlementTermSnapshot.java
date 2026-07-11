package com.tchalanet.server.core.sales.api.model.settlement;

import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import java.util.Objects;

public record SettlementTermSnapshot(
    SettlementRuleCode ruleCode,
    Short sourceBetOption,
    String commercialLabel,
    PayoutRuleSnapshot payoutRule,
    java.math.BigDecimal payoutBaseAmount,
    SettlementWinMode winMode,
    SettlementTermSource source
) {

    public SettlementTermSnapshot {
        Objects.requireNonNull(ruleCode, "settlement.term.rule_code_required");
        Objects.requireNonNull(payoutRule, "settlement.term.payout_rule_required");
        if (payoutRule.type() == PayoutRuleType.STAKE_MULTIPLIER) {
            Objects.requireNonNull(payoutBaseAmount, "settlement.term.payout_base_required");
            if (payoutBaseAmount.signum() < 0) {
                throw new IllegalArgumentException("settlement.term.payout_base_must_be_non_negative");
            }
        } else {
            payoutBaseAmount = null;
        }
        winMode = winMode == null ? SettlementWinMode.ALTERNATIVE : winMode;
        source = source == null ? SettlementTermSource.TENANT_DEFAULT : source;
        commercialLabel = normalize(commercialLabel);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
