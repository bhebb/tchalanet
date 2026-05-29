package com.tchalanet.server.features.cashier.home.model;

import java.util.Map;

public record CashierReadinessBlocker(
    String type,
    String titleKey,
    String messageKey,
    Map<String, Object> params
) {
    public CashierReadinessBlocker {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
