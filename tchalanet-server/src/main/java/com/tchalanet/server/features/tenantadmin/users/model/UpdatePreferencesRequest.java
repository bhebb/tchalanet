package com.tchalanet.server.features.tenantadmin.users.model;

import com.tchalanet.server.common.types.enums.ThemeMode;

public record UpdatePreferencesRequest(
    ThemeMode themeMode,
    Short density,
    String locale,
    String timeZone,
    String currency
) {}
