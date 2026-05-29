package com.tchalanet.server.features.cashier.tickets.model;

import java.util.List;
import java.util.Map;

public record CashierTicketVerificationResponse(
    String status,
    String severity,
    String titleKey,
    String messageKey,
    Map<String, Object> params,
    List<CashierAction> availableActions
) {
    public CashierTicketVerificationResponse {
        params = params == null ? Map.of() : Map.copyOf(params);
        availableActions = availableActions == null ? List.of() : List.copyOf(availableActions);
    }
}
