package com.tchalanet.server.features.bootstrap.publicruntime.model;

import java.util.Map;

public record RuntimeThemeView(
    String presetCode,
    String mode,
    Map<String, String> tokens,
    boolean isDefault,
    long version
) {
    public static RuntimeThemeView fallback() {
        return new RuntimeThemeView("default", "light", Map.of(), true, 0L);
    }
}
