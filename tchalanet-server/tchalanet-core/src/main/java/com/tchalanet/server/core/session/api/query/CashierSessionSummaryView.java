package com.tchalanet.server.core.session.api.query;

import java.time.Instant;

/**
 * Public-safe view of a cashier's current session for the dashboard widget.
 * Does not expose internal IDs.
 * When active=false, all other fields are null/zero.
 */
public record CashierSessionSummaryView(
    boolean active,
    String sessionRef,
    Instant openedAt,
    long openingFloatCents,
    long salesTotalCents,
    int ticketCount
) {
    public static CashierSessionSummaryView inactive() {
        return new CashierSessionSummaryView(false, null, null, 0L, 0L, 0);
    }
}
