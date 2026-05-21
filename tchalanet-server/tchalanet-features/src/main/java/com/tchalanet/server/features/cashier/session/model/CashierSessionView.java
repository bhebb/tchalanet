package com.tchalanet.server.features.cashier.session.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cashier-facing projection of {@link SalesSession}. The core query returns the domain aggregate
 * directly, so this view shields the feature API from internal type changes.
 */
public record CashierSessionView(
    SalesSessionId sessionId,
    OutletId outletId,
    TerminalId terminalId,
    UserId openedBy,
    String status,
    Instant openedAt,
    Instant closedAt,
    BigDecimal openingFloat,
    UserId closedBy,
    BigDecimal closingAmount
) {
    public static CashierSessionView from(SalesSession s) {
        if (s == null) {
            return null;
        }
        return new CashierSessionView(
            s.id(),
            s.outletId(),
            s.terminalId(),
            s.openedBy(),
            s.status() != null ? s.status().name() : null,
            s.openedAt(),
            s.closedAt(),
            cents(s.openingFloatCents()),
            s.closedBy(),
            cents(s.closingAmountCents())
        );
    }

    private static BigDecimal cents(Long c) {
        return c == null ? null : BigDecimal.valueOf(c, 2);
    }
}
