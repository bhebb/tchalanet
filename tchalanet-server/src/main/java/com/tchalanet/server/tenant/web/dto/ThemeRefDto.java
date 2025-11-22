package com.tchalanet.server.tenant.web.dto;

import com.tchalanet.server.tenant.domain.model.ThemeMode;

public record ThemeRefDto(String presetId, ThemeMode mode, Short density) {}
