package com.tchalanet.server.features.pos.home.model;

import java.util.Map;

public record PosBadge(
    String type,
    PosAttentionLevel attentionLevel,
    String titleKey,
    Map<String, Object> params
) {
    public PosBadge {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
