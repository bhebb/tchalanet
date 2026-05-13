package com.tchalanet.server.core.sales.internal.domain.service;

import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;

public class TicketResultPolicy {

  public boolean canRecordResult(TicketSaleStatus saleStatus) {
    return saleStatus == TicketSaleStatus.SOLD;
  }

  public boolean canOverride(TicketSaleStatus saleStatus) {
    return saleStatus != TicketSaleStatus.VOID;
  }

  public boolean isResulted(TicketResultStatus resultStatus) {
    return resultStatus != TicketResultStatus.NOT_RESULTED;
  }
}
