package com.tchalanet.server.core.session.application.query.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.math.BigDecimal;
import java.time.Instant;

/** DTO for session with totals. */
public record SessionWithTotalsDto(
    SessionId sessionId,
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    UserId userId,
    String status,
    Instant openedAt,
    Instant closedAt,
    BigDecimal openingFloat,
    BigDecimal closingAmount,
    SalesSessionTotalsDto totals) {
  public record SalesSessionTotalsDto(
      long totalTickets,
      BigDecimal totalStake,
      BigDecimal totalPayout,
      BigDecimal grossMargin,
      Instant updatedAt) {}
}
