package com.tchalanet.server.features.runtime.model;

import jakarta.annotation.Nullable;

/** A single public navigation entry (header or footer). */
public record PublicNavigationItem(
    String id,
    String labelKey,
    String route,
    @Nullable Boolean external
) {
    public static PublicNavigationItem of(String id, String labelKey, String route) {
        return new PublicNavigationItem(id, labelKey, route, null);
    }
}
