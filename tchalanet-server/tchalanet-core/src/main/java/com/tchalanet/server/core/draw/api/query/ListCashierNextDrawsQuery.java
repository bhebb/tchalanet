package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;

import java.util.List;

/**
 * Lists upcoming draws for the cashier dashboard, mapped to a public-safe view.
 * Limit is clamped 1..20 by the handler.
 */
public record ListCashierNextDrawsQuery(
    int lookaheadHours,
    int limit
) implements Query<List<CashierNextDrawView>> {}
