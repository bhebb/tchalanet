package com.tchalanet.server.common.web.dto;

import com.tchalanet.server.tenant.web.dto.ThemeRefDto;
import com.tchalanet.server.user.web.dto.UserPrefDto;

public record MeThemeResponse(ThemeRefDto theme, UserPrefDto user) {}
