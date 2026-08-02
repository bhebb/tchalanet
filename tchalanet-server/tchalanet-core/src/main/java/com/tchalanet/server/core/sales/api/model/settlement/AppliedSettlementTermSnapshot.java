package com.tchalanet.server.core.sales.api.model.settlement;

import java.math.BigDecimal;
import java.util.Objects;

/** A captured settlement rule together with the realized payout it produced. */
public record AppliedSettlementTermSnapshot(
    SettlementRuleCode ruleCode, SettlementTermSource source, BigDecimal realizedAmount) {

  public AppliedSettlementTermSnapshot {
    Objects.requireNonNull(ruleCode, "applied_settlement.rule_code_required");
    Objects.requireNonNull(source, "applied_settlement.source_required");
    if (realizedAmount == null || realizedAmount.signum() < 0) {
      throw new IllegalArgumentException("applied_settlement.realized_amount_invalid");
    }
  }
}
