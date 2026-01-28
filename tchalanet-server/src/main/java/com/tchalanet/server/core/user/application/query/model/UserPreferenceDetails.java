package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.types.enums.ThemeMode;

public record UserPreferenceDetails(
    ThemeMode themeMode,
    Short density,
    String locale,
    String timeZone,
    String currency) {}
