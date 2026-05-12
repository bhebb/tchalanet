package com.tchalanet.server.core.sales.internal.domain.service;

import com.tchalanet.server.core.sales.domain.model.TicketSaleStatus;

public class TicketLifecyclePolicy {

  public boolean canApprove(TicketSaleStatus status) {
    return status == TicketSaleStatus.PENDING_APPROVAL;
  }

  public boolean canReject(TicketSaleStatus status) {
    return status == TicketSaleStatus.PENDING_APPROVAL;
  }

  public boolean canCancel(TicketSaleStatus status) {
    return status == TicketSaleStatus.SOLD || status == TicketSaleStatus.PENDING_APPROVAL;
  }
}

