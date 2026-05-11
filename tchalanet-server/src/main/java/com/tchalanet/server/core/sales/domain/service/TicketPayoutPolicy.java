package com.tchalanet.server.core.sales.domain.service;

import com.tchalanet.server.core.sales.domain.model.TicketResultStatus;
import com.tchalanet.server.core.sales.domain.model.TicketSettlementStatus;

public class TicketPayoutPolicy {

  public boolean canMarkPaid(TicketResultStatus resultStatus, TicketSettlementStatus settlementStatus) {
    return resultStatus != TicketResultStatus.NOT_RESULTED && settlementStatus != TicketSettlementStatus.PAID_OUT;
  }
}

