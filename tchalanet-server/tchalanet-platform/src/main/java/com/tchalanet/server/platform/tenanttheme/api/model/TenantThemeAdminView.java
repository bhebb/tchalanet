package com.tchalanet.server.platform.tenanttheme.api.model;

import java.time.Instant;

/**
 * Admin view for tenant theme — returned to TENANT_ADMIN.
 * No raw preset config exposed.
 */
public record TenantThemeAdminView(
    String presetCode,
    String defaultMode,
    boolean active,
    boolean isDefault,
    long version,
    Instant updatedAt
) {}
