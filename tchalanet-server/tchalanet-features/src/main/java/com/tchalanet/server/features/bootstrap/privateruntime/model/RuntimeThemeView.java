package com.tchalanet.server.features.bootstrap;

import java.util.Map;

public record RuntimeThemeView(
    String presetCode,
    String mode,
    Map<String, String> tokens,
    boolean isDefault,
    long version
) {
    public static com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeThemeView fallback() {
        return new com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeThemeView("default", "light", Map.of(), true, 0L);
    }
}
