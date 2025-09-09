package com.tchalanet.server.dto;

import com.tchalanet.server.constants.ThemeMode;

public record PreferenceUpdateDto(ThemeMode themeMode, Short density, String locale) {}
