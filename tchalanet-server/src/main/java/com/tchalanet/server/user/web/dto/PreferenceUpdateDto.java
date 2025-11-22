package com.tchalanet.server.user.web.dto;

import com.tchalanet.server.user.domain.model.ThemeMode;
import java.util.Locale;

public record PreferenceUpdateDto(ThemeMode themeMode, Short density, Locale locale) {}
