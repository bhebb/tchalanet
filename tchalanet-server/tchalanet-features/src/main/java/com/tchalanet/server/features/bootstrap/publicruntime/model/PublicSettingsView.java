package com.tchalanet.server.features.bootstrap.publicruntime.model;

import java.util.List;
import java.util.Map;

/** Public-safe runtime settings for unauthenticated pages. */
public record PublicSettingsView(
    String locale,
    String timezone,
    List<String> supportedLocales,
    String defaultCurrency,
    Map<String, Boolean> features
) {}
