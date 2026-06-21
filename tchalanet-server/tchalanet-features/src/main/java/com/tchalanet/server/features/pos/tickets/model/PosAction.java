package com.tchalanet.server.features.pos.tickets.model;

import java.util.Map;

public record PosAction(
    PosActionType type,
    String labelKey,
    boolean enabled,
    Map<String, Object> params
) {
    public PosAction {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public static PosAction enabled(PosActionType type, String labelKey, Map<String, Object> params) {
        return new PosAction(type, labelKey, true, params);
    }

    public static PosAction disabled(PosActionType type, String labelKey, Map<String, Object> params) {
        return new PosAction(type, labelKey, false, params);
    }
}
