package com.tchalanet.server.core.user.web.dto;

import com.tchalanet.server.core.user.domain.model.ThemeMode;

public record UserPrefDto(ThemeMode mode, Short density, String locale) {}
