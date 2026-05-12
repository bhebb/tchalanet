package com.tchalanet.server.core.session.internal.infra.web.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;

import java.math.BigDecimal;
import java.time.Instant;

public record SalesSessionResponse(
    SalesSessionId id,
    OutletId outletId,
    TerminalId terminalId,
    UserId openedBy,
    String status,
    Instant openedAt,
    Instant closedAt,
    BigDecimal openingFloat,
    UserId closedBy,
    BigDecimal closingAmount) {

    public static SalesSessionResponse fromDomain(SalesSession s) {
        if (s == null) {
            return null;
        }

        return new SalesSessionResponse(
            s.id(),
            s.outletId(),
            s.terminalId(),
            s.openedBy(),
            s.status() != null ? s.status().name() : null,
            s.openedAt(),
            s.closedAt(),
            centsToAmount(s.openingFloatCents()),
            s.closedBy(),
            centsToAmount(s.closingAmountCents()));
    }

    private static BigDecimal centsToAmount(Long cents) {
        return cents == null ? null : BigDecimal.valueOf(cents, 2);
    }
}
