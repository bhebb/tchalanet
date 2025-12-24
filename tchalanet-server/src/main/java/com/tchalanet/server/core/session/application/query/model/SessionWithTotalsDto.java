package com.tchalanet.server.core.session.application.query.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for session with totals.
 */
public record SessionWithTotalsDto(
    UUID sessionId,
    UUID tenantId,
    UUID outletId,
    UUID terminalId,
    UUID userId,
    String status,
    Instant openedAt,
    Instant closedAt,
    BigDecimal openingFloat,
    BigDecimal closingAmount,
    PosSessionTotalsDto totals
) {
  public record PosSessionTotalsDto(
      long totalTickets,
      BigDecimal totalStake,
      BigDecimal totalPayout,
      BigDecimal grossMargin,
      Instant updatedAt
  ) {}
}
