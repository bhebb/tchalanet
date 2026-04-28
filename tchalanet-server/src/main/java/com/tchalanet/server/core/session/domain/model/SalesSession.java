package com.tchalanet.server.core.session.domain.model;

import com.tchalanet.server.common.types.id.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record SalesSession(
    SessionId id,
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    UserId userId,
    SalesSessionStatus status,
    Instant openedAt,
    Instant closedAt,
    Long openingFloatCents, // nullable
    Long closingAmountCents, // nullable
    BigDecimal totalStake,
    Long totalTickets,
    BigDecimal totalPayout,
    String meta,
    long version) {

  public static SalesSession open(
      SessionId id,
      TenantId tenantId,
      OutletId outletId,
      TerminalId terminalId,
      UserId userId,
      Long openingFloatCents,
      Instant now) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(tenantId);
    Objects.requireNonNull(outletId);
    Objects.requireNonNull(terminalId);
    Objects.requireNonNull(userId);
    Objects.requireNonNull(now);

    return new SalesSession(
        id,
        tenantId,
        outletId,
        terminalId,
        userId,
        SalesSessionStatus.OPENED,
        now,
        null,
        openingFloatCents,
        null,
        BigDecimal.ZERO,
        0L,
        BigDecimal.ZERO,
        null,
        0L);
  }

  public SalesSession close(BigDecimal closingAmountCents, Instant now) {
    if (status != SalesSessionStatus.OPENED) {
      throw new IllegalStateException("Only OPEN session can be closed. status=" + status);
    }
    Objects.requireNonNull(now);
    return new SalesSession(
        id,
        tenantId,
        outletId,
        terminalId,
        userId,
        SalesSessionStatus.CLOSED,
        openedAt,
        now,
        openingFloatCents,
        closingAmountCents.longValue(),
        totalStake,
        totalTickets,
        totalPayout,
        meta,
        version);
  }

  public static SalesSession reconstruct(
      SessionId id,
      TenantId tenantId,
      OutletId outletId,
      TerminalId terminalId,
      UserId userId,
      SalesSessionStatus status,
      Instant openedAt,
      Instant closedAt,
      Long openingFloatCents,
      Long closingAmountCents,
      BigDecimal totalStake,
      Long totalTickets,
      BigDecimal totalPayout,
      String meta,
      long version) {
    return new SalesSession(
        id,
        tenantId,
        outletId,
        terminalId,
        userId,
        status,
        openedAt,
        closedAt,
        openingFloatCents,
        closingAmountCents,
        totalStake,
        totalTickets,
        totalPayout,
        meta,
        version);
  }
}
