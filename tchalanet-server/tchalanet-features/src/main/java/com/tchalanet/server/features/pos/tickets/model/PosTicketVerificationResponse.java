package com.tchalanet.server.features.pos.tickets.model;

import java.util.List;
import java.util.Map;

public record PosTicketVerificationResponse(
    String status,
    String severity,
    String titleKey,
    String messageKey,
    Map<String, Object> params,
    List<PosAction> availableActions
) {
    public PosTicketVerificationResponse {
        params = params == null ? Map.of() : Map.copyOf(params);
        availableActions = availableActions == null ? List.of() : List.copyOf(availableActions);
    }
}
