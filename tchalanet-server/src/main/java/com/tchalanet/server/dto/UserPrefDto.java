package com.tchalanet.server.dto;

import com.tchalanet.server.constants.ThemeMode;

public record UserPrefDto(ThemeMode mode, Short density, String locale) {}
