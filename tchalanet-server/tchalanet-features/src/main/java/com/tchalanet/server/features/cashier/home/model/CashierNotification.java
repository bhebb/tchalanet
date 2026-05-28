package com.tchalanet.server.features.cashier.home.model;

import java.util.Map;

public record CashierNotification(
    String type,
    CashierAttentionLevel attentionLevel,
    String titleKey,
    String messageKey,
    String actionType,
    String actionKey,
    Map<String, Object> params
) {
    public CashierNotification {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
