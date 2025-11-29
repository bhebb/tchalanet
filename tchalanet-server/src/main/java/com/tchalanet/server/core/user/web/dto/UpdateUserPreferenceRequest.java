package com.tchalanet.server.core.user.web.dto;

import com.tchalanet.server.core.user.domain.model.ThemeMode;

public record UpdateUserPreferenceRequest(ThemeMode themeMode, Short density, String locale) {}
