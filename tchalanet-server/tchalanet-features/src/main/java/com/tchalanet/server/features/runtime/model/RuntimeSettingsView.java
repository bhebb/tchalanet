package com.tchalanet.server.features.runtime.model;

import java.util.Map;

public record RuntimeSettingsView(
    String locale,
    String timezone,
    String currency,
    Map<String, Boolean> features
) {}
