package com.tchalanet.server.core.session.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PosSession(
    UUID id,
    UUID tenantId,
    UUID outletId,
    UUID terminalId,
    UUID userId,
    PosSessionStatus status,
    Instant openedAt,
    Instant closedAt,
    BigDecimal openingFloat,
    BigDecimal closingAmount,
    Long totalTickets,
    BigDecimal totalStake,
    BigDecimal totalPayout,
    BigDecimal grossMargin,
    Map<String, Object> meta,
    long version) {

  public static PosSession open(
      UUID id,
      UUID tenantId,
      UUID outletId,
      UUID terminalId,
      UUID userId,
      BigDecimal openingFloat,
      Instant now) {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(outletId, "outletId is required");
    Objects.requireNonNull(terminalId, "terminalId is required");
    Objects.requireNonNull(userId, "userId is required");
    Objects.requireNonNull(now, "now is required");

    return new PosSession(
        id,
        tenantId,
        outletId,
        terminalId,
        userId,
        PosSessionStatus.OPEN,
        now,
        null,
        openingFloat,
        null,
        null,
        null,
        null,
        null,
        Map.of(),
        0L);
  }

  public PosSession close(BigDecimal closingAmount, Instant now) {
    if (status != PosSessionStatus.OPEN) {
      throw new IllegalStateException(
          "Only an OPEN session can be closed. Current status: " + status);
    }
    Objects.requireNonNull(now, "now is required");

    return new PosSession(
        id,
        tenantId,
        outletId,
        terminalId,
        userId,
        PosSessionStatus.CLOSED,
        openedAt,
        now,
        openingFloat,
        closingAmount,
        totalTickets,
        totalStake,
        totalPayout,
        grossMargin,
        meta,
        version);
  }
}
