package com.tchalanet.server.features.bootstrap;

import java.util.Map;

public record RuntimeI18nBundle(
    String locale,
    Map<String, String> messages
) {
    public static com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeI18nBundle empty(String locale) {
        return new com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeI18nBundle(locale, Map.of());
    }
}
