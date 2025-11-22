package com.tchalanet.server.user.web.dto;

import com.tchalanet.server.user.domain.model.ThemeMode;

public record UpdateUserPreferenceRequest(ThemeMode themeMode, Short density, String locale) {}
