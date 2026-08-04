package com.tchalanet.server.core.sales.api.model.status;

public enum TicketSaleStatus {
  APPROVED,
  CANCELLED,
  VOIDED;

  public boolean isAcceptedSale() {
    return this == APPROVED;
  }

  public boolean isPrintable() {
    return this == APPROVED || this == CANCELLED;
  }

  public boolean isCancellable() {
    return this == APPROVED;
  }

  public boolean isFinal() {
    return this == CANCELLED || this == VOIDED;
  }
}
