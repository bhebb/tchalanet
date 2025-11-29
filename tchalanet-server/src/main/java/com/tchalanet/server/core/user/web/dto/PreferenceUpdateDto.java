package com.tchalanet.server.core.user.web.dto;

import com.tchalanet.server.core.user.domain.model.ThemeMode;
import java.util.Locale;

public record PreferenceUpdateDto(ThemeMode themeMode, Short density, Locale locale) {}
