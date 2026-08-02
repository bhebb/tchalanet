package com.tchalanet.server.core.sales.api.model.line;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.settlement.AppliedSettlementSnapshot;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;

public record TicketLineResult(
    TicketLineResultStatus status,
    Money payoutAmount,
    AppliedSettlementSnapshot appliedSettlementSnapshot) {

  public TicketLineResult(TicketLineResultStatus status, Money payoutAmount) {
    this(status, payoutAmount, null);
  }
}
