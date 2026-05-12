package com.tchalanet.server.platform.identity.internal.web.model;

import com.tchalanet.server.common.types.enums.ThemeMode;

public record UserPreferenceRequest(
    ThemeMode themeMode,
    Short density,
    String locale,
    String timeZone,
    String currency) {}
