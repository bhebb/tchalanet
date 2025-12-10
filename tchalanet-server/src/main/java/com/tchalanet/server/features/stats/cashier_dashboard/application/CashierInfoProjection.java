package com.tchalanet.server.features.stats.cashier_dashboard.application;

import java.util.UUID;

public record CashierInfoProjection(
    UUID cashierId,
    String cashierName,
    UUID outletId,
    String outletName
) {
}
