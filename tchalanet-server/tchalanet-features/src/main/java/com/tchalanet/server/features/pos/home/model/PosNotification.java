package com.tchalanet.server.features.pos.home.model;

import java.util.Map;

public record PosNotification(
    String type,
    PosAttentionLevel attentionLevel,
    String titleKey,
    String messageKey,
    String actionType,
    String actionKey,
    Map<String, Object> params
) {
    public PosNotification {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
