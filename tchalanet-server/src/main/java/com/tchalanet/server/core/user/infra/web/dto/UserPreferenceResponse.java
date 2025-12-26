package com.tchalanet.server.core.user.infra.web.dto;
import com.tchalanet.server.common.types.enums.ThemeMode;
import com.tchalanet.server.common.types.id.UserId;

import java.util.UUID;

public record UserPreferenceResponse(
    UUID userId, ThemeMode themeMode, Short density, String locale) {}
