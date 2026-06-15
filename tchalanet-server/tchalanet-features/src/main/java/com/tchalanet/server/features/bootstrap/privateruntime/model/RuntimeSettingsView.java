package com.tchalanet.server.features.bootstrap;

import java.util.Map;

public record RuntimeSettingsView(
    String locale,
    String timezone,
    String currency,
    Map<String, Boolean> features
) {}
