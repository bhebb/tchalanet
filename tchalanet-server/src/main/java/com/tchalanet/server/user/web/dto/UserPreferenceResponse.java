package com.tchalanet.server.user.web.dto;

import com.tchalanet.server.user.domain.model.ThemeMode;
import java.util.UUID;

public record UserPreferenceResponse(
    UUID userId, ThemeMode themeMode, Short density, String locale) {}
