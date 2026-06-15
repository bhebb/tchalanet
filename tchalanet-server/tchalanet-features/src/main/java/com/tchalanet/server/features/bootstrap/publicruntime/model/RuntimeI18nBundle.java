package com.tchalanet.server.features.bootstrap.publicruntime.model;

import java.util.Map;

public record RuntimeI18nBundle(
    String locale,
    Map<String, String> messages
) {
    public static RuntimeI18nBundle empty(String locale) {
        return new RuntimeI18nBundle(locale, Map.of());
    }
}
