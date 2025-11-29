package com.tchalanet.server.core.user.web.dto;

import com.tchalanet.server.core.user.domain.model.ThemeMode;
import java.util.UUID;

public record UserPreferenceResponse(
    UUID userId, ThemeMode themeMode, Short density, String locale) {}
