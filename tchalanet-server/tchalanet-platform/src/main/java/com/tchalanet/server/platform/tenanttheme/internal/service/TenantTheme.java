package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

/**
 * Domain model for tenant-scoped theme.
 * No free-form metadata — token overrides deferred to V2.
 */
public record TenantTheme(
    TenantId tenantId,
    String presetCode,
    String defaultMode,
    boolean active,
    boolean isDefault,
    long version,
    Instant createdAt,
    Instant updatedAt,
    String createdBy
) {
    public TenantTheme {
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (presetCode == null || presetCode.isBlank()) throw new IllegalArgumentException("presetCode is required");
        if (version < 0) throw new IllegalArgumentException("version must be >= 0");
        if (defaultMode == null || defaultMode.isBlank()) defaultMode = "SYSTEM";
    }
}
