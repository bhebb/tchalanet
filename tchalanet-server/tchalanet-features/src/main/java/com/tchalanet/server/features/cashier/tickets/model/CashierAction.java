package com.tchalanet.server.features.cashier.tickets.model;

import java.util.Map;

public record CashierAction(
    CashierActionType type,
    String labelKey,
    boolean enabled,
    Map<String, Object> params
) {
    public CashierAction {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public static CashierAction enabled(CashierActionType type, String labelKey, Map<String, Object> params) {
        return new CashierAction(type, labelKey, true, params);
    }

    public static CashierAction disabled(CashierActionType type, String labelKey, Map<String, Object> params) {
        return new CashierAction(type, labelKey, false, params);
    }
}
