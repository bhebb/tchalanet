package com.tchalanet.server.platform.identity.internal.web.admin.model;

import com.tchalanet.server.common.types.enums.ThemeMode;

public record UpdatePreferencesRequest(
    ThemeMode themeMode,
    Short density,
    String locale,
    String timeZone,
    String currency
) {}
