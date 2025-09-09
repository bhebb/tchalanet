package com.tchalanet.server.dto;

import com.tchalanet.server.constants.ThemeMode;

public record ThemeRefDto(String presetId, ThemeMode mode, Short density) {}
