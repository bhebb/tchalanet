package com.tchalanet.server.core.tenant.web.dto;

import com.tchalanet.server.core.tenant.domain.model.ThemeMode;

public record ThemeRefDto(String presetId, ThemeMode mode, Short density) {}
