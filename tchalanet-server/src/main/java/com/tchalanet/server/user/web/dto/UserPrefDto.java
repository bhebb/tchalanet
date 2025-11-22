package com.tchalanet.server.user.web.dto;

import com.tchalanet.server.user.domain.model.ThemeMode;

public record UserPrefDto(ThemeMode mode, Short density, String locale) {}
