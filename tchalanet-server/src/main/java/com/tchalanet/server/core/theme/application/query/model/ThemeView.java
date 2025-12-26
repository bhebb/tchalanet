package com.tchalanet.server.core.theme.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.types.enums.ThemeMode;
import com.tchalanet.server.core.theme.domain.model.Theme;
import com.tchalanet.server.core.theme.domain.model.ThemeStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ThemeView(
    UUID id,
    TenantId tenantId,
    String basePresetId,
    String label,
    ThemeMode mode,
    short density,
    Map<String, Object> palette,
    Map<String, Object> tokens,
    Map<String, String> cssVars,
    ThemeStatus status,
    Instant createdAt,
    Instant updatedAt) {

    public static ThemeView fromDomain(Theme theme) {
        return new ThemeView(
            theme.id(),
            theme.tenantId(),
            theme.basePresetId(),
            theme.label(),
            theme.mode(),
            theme.density(),
            theme.palette(),
            theme.tokens(),
            theme.cssVars(),
            theme.status(),
            theme.createdAt(),
            theme.updatedAt());
    }
}
