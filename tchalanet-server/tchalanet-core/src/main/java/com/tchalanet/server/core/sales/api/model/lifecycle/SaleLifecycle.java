package com.tchalanet.server.core.sales.api.model.lifecycle;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.time.Instant;

public record SaleLifecycle(
    TicketSaleStatus status,
    Instant soldAt,
    Instant placedAt,
    DecisionTrace cancellation,
    DecisionTrace voiding) {
  public static SaleLifecycle initial(TicketSaleStatus status, Instant now) {
    return new SaleLifecycle(status, now, now, null, null);
  }

  public SaleLifecycle cancelled(UserId by, String reason, Instant now) {
    return new SaleLifecycle(
        TicketSaleStatus.CANCELLED, soldAt, placedAt, new DecisionTrace(now, by, reason), voiding);
  }

  public SaleLifecycle voided(UserId by, String reason, Instant now) {
    return new SaleLifecycle(
        TicketSaleStatus.VOIDED,
        soldAt,
        placedAt,
        cancellation,
        new DecisionTrace(now, by, reason));
  }
}
