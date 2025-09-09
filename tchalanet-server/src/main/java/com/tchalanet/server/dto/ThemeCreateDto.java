package com.tchalanet.server.dto;

import com.tchalanet.server.constants.ThemeMode;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record ThemeCreateDto(
    @NotBlank String basePresetId,
    @NotBlank String label,
    ThemeMode mode,
    Short density,
    Map<String, Object> palette,
    Map<String, Object> tokens,
    Map<String, String> cssVars) {}
