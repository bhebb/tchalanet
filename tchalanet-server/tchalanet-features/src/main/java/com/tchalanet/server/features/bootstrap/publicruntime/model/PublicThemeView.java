package com.tchalanet.server.features.bootstrap.publicruntime.model;

import java.util.Map;

/**
 * Public runtime theme — tokens for CSS variables, served from the global default preset.
 * No tenant context; the seed sets "tchalanet" as the platform default.
 */
public record PublicThemeView(
    String presetCode,
    String mode,
    Map<String, String> tokens
) {
    public static PublicThemeView fallback() {
        return new PublicThemeView("tchalanet", "light", Map.of());
    }
}
