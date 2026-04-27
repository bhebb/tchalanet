package com.tchalanet.server.core.session.infra.web.model;

import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.math.BigDecimal;
import java.time.Instant;

public record PosSessionResponse(
    SessionId id,
    TenantId tenantId,
    String outletId,
    TerminalId terminalId,
    UserId userId,
    String status,
    Instant openedAt,
    Instant closedAt,
    BigDecimal openingFloat,
    BigDecimal closingAmount) {

  public static PosSessionResponse fromDomain(
      com.tchalanet.server.core.session.domain.model.PosSession s) {
    if (s == null) return null;

    BigDecimal opening =
        s.openingFloatCents() == null
            ? null
            : BigDecimal.valueOf(s.openingFloatCents()).movePointLeft(2);
    BigDecimal closing =
        s.closingAmountCents() == null
            ? null
            : BigDecimal.valueOf(s.closingAmountCents()).movePointLeft(2);

    return new PosSessionResponse(
        s.id(),
        s.tenantId(),
        s.outletId() == null ? null : s.outletId().value().toString(),
        s.terminalId(),
        s.userId(),
        s.status() == null ? null : s.status().name(),
        s.openedAt(),
        s.closedAt(),
        opening,
        closing);
  }
}
