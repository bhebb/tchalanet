package com.tchalanet.server.user.web.dto;

import com.tchalanet.server.tenant.web.dto.ThemeRefDto;

public record MeThemeResponse(ThemeRefDto theme, UserPrefDto user) {}
