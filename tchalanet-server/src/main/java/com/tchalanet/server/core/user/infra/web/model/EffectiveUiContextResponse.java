package com.tchalanet.server.core.user.infra.web.model;

import com.tchalanet.server.common.types.enums.ThemeMode;

public record EffectiveUiContextResponse(
    ThemeMode themeMode, Short density, String locale, String timeZone, String currency) {}
