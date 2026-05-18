package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;

import java.time.LocalDate;

public record ListCashierTopSelectionsQuery(
    UserId cashierId,
    LocalDate businessDate,
    int limitPerDraw
) implements Query<CashierTopSelectionsView> {}
