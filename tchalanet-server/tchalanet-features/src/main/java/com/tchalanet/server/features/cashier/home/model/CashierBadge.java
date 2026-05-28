package com.tchalanet.server.features.cashier.home.model;

import java.util.Map;

public record CashierBadge(
    String type,
    CashierAttentionLevel attentionLevel,
    String titleKey,
    Map<String, Object> params
) {
    public CashierBadge {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
