package com.tchalanet.server.core.user.web.dto;

import com.tchalanet.server.core.tenant.web.dto.ThemeRefDto;

public record MeThemeResponse(ThemeRefDto theme, UserPrefDto user) {}
