package com.tchalanet.server.core.user.infra.web.dto;

import java.util.UUID;

public record UserPreferenceResponse(
    UUID userId, ThemeMode themeMode, Short density, String locale) {}
