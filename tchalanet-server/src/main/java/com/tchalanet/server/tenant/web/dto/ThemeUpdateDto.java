package com.tchalanet.server.tenant.web.dto;

import com.tchalanet.server.tenant.domain.model.ThemeMode;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ThemeUpdateDto(
    @NotNull Integer version,
    String label,
    ThemeMode mode,
    Short density,
    Map<String, Object> palette,
    Map<String, Object> tokens,
    Map<String, String> cssVars) {}
