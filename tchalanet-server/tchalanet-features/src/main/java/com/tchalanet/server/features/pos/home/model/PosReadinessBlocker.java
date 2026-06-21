package com.tchalanet.server.features.pos.home.model;

import java.util.Map;

public record PosReadinessBlocker(
    String type,
    String titleKey,
    String messageKey,
    Map<String, Object> params
) {
    public PosReadinessBlocker {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
